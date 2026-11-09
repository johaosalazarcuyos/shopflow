package dev.harold.shopflow;

import dev.harold.shopflow.notification.NotificationRepository;
import dev.harold.shopflow.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderFlowIntegrationTest extends IntegrationTest {

    @Autowired
    NotificationRepository notifications;

    @Test
    void fullPurchaseLifecycle() throws Exception {
        Session admin = loginAdmin();
        Session customer = registerCustomer();
        Long productId = createProduct(admin, "89.90", 10);

        addToCart(customer, productId, 2).andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(179.8));

        String order = checkoutRequest(customer)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.subtotal").value(179.8))
                .andExpect(jsonPath("$.shippingCost").value(10.0))
                .andExpect(jsonPath("$.total").value(189.8))
                .andExpect(jsonPath("$.orderNumber").value(org.hamcrest.Matchers.startsWith("SF-")))
                .andReturn().getResponse().getContentAsString();
        Long orderId = longAt(order, "$.id");

        assertThat(stockOf(productId)).isEqualTo(8);
        mvc.perform(get("/api/cart").header("Authorization", customer.bearer()))
                .andExpect(jsonPath("$.items", hasSize(0)));

        pay(customer, orderId, UUID.randomUUID().toString(), "tok_visa")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.amount").value(189.8));

        setStatus(admin, orderId, "SHIPPED").andExpect(status().isOk());
        setStatus(admin, orderId, "DELIVERED").andExpect(status().isOk())
                .andExpect(jsonPath("$.history", hasSize(4)))
                .andExpect(jsonPath("$.history[1].to").value("PAID"))
                .andExpect(jsonPath("$.history[3].changedBy").value("admin@shopflow.dev"));

        // Las notificaciones se envían de forma asíncrona tras el commit
        await(() -> notifications.findByRecipientOrderByIdAsc(customer.email()).size() == 4);
        assertThat(notifications.findByRecipientOrderByIdAsc(customer.email()))
                .extracting(n -> n.getType())
                .containsExactlyInAnyOrder("ORDER_PLACED", "ORDER_PAID", "ORDER_SHIPPED", "ORDER_DELIVERED");
    }

    @Test
    void freeShippingAboveThreshold() throws Exception {
        Session admin = loginAdmin();
        Session customer = registerCustomer();
        Long productId = createProduct(admin, "150.00", 5);
        addToCart(customer, productId, 2);
        checkoutRequest(customer).andExpect(jsonPath("$.shippingCost").value(0.0))
                .andExpect(jsonPath("$.total").value(300.0));
    }

    @Test
    void declinedPaymentKeepsOrderPendingAndCanBeRetried() throws Exception {
        Session admin = loginAdmin();
        Session customer = registerCustomer();
        addToCart(customer, createProduct(admin, "50.00", 5), 1);
        Long orderId = checkout(customer);

        pay(customer, orderId, UUID.randomUUID().toString(), "tok_declined")
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("card_declined"));
        mvc.perform(get("/api/orders/{id}", orderId).header("Authorization", customer.bearer()))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));

        pay(customer, orderId, UUID.randomUUID().toString(), "tok_mastercard").andExpect(status().isCreated());
    }

    @Test
    void paymentsAreIdempotent() throws Exception {
        Session admin = loginAdmin();
        Session customer = registerCustomer();
        addToCart(customer, createProduct(admin, "20.00", 5), 1);
        Long orderId = checkout(customer);
        String key = UUID.randomUUID().toString();

        String first = pay(customer, orderId, key, "tok_visa").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String retry = pay(customer, orderId, key, "tok_visa").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(longAt(retry, "$.id")).isEqualTo(longAt(first, "$.id"));
        assertThat(jdbc.queryForObject("select count(*) from payments where order_id = ?", Integer.class, orderId))
                .isEqualTo(1);

        pay(customer, orderId, UUID.randomUUID().toString(), "tok_visa")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_PAYABLE"));
    }

    @Test
    void checkoutFailsAtomicallyWhenStockRunsOut() throws Exception {
        Session admin = loginAdmin();
        Session customer = registerCustomer();
        Long ok = createProduct(admin, "10.00", 5);
        Long scarce = createProduct(admin, "10.00", 2);
        addToCart(customer, ok, 1);
        addToCart(customer, scarce, 2);

        // Otro canal vende el stock mientras el producto está en el carrito
        jdbc.update("update products set stock = 1 where id = ?", scarce);

        checkoutRequest(customer)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.items[0].productId").value(scarce))
                .andExpect(jsonPath("$.items[0].available").value(1));

        assertThat(stockOf(ok)).isEqualTo(5); // nada se descontó
        mvc.perform(get("/api/cart").header("Authorization", customer.bearer()))
                .andExpect(jsonPath("$.items", hasSize(2)));
    }

    @Test
    void cannotAddMoreThanAvailableOrCheckoutEmptyCart() throws Exception {
        Session admin = loginAdmin();
        Session customer = registerCustomer();
        addToCart(customer, createProduct(admin, "10.00", 3), 4)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
        checkoutRequest(customer).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("EMPTY_CART"));
    }

    @Test
    void cancellingPaidOrderRefundsAndRestocks() throws Exception {
        Session admin = loginAdmin();
        Session customer = registerCustomer();
        Long productId = createProduct(admin, "30.00", 10);
        addToCart(customer, productId, 3);
        Long orderId = checkout(customer);
        pay(customer, orderId, UUID.randomUUID().toString(), "tok_visa");
        assertThat(stockOf(productId)).isEqualTo(7);

        mvc.perform(post("/api/orders/{id}/cancel", orderId).header("Authorization", customer.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Me equivoqué de modelo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(stockOf(productId)).isEqualTo(10);
        assertThat(jdbc.queryForObject("select status from payments where order_id = ?", String.class, orderId))
                .isEqualTo("REFUNDED");
    }

    @Test
    void enforcesStateMachine() throws Exception {
        Session admin = loginAdmin();
        Session customer = registerCustomer();
        addToCart(customer, createProduct(admin, "30.00", 10), 1);
        Long orderId = checkout(customer);

        setStatus(admin, orderId, "DELIVERED").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
        setStatus(admin, orderId, "PAID").andExpect(status().isUnprocessableContent());

        pay(customer, orderId, UUID.randomUUID().toString(), "tok_visa");
        setStatus(admin, orderId, "SHIPPED").andExpect(status().isOk());
        mvc.perform(post("/api/orders/{id}/cancel", orderId).header("Authorization", customer.bearer()))
                .andExpect(status().isConflict());
    }

    @Test
    void customersOnlySeeTheirOwnOrders() throws Exception {
        Session admin = loginAdmin();
        Session alice = registerCustomer();
        Session bob = registerCustomer();
        addToCart(alice, createProduct(admin, "30.00", 10), 1);
        Long orderId = checkout(alice);

        mvc.perform(get("/api/orders/{id}", orderId).header("Authorization", bob.bearer()))
                .andExpect(status().isNotFound());
        pay(bob, orderId, UUID.randomUUID().toString(), "tok_visa").andExpect(status().isNotFound());
        mvc.perform(get("/api/orders").header("Authorization", bob.bearer()))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void validatesShippingAddress() throws Exception {
        Session customer = registerCustomer();
        mvc.perform(post("/api/orders").header("Authorization", customer.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shippingAddress":{"fullName":"A","line1":"B","district":"C","city":"D","phone":"123"}}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['shippingAddress.phone']").exists());
    }
}
