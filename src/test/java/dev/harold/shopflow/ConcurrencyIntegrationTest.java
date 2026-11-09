package dev.harold.shopflow;

import dev.harold.shopflow.order.InsufficientStockException;
import dev.harold.shopflow.order.OrderDtos.AddressRequest;
import dev.harold.shopflow.order.OrderDtos.CheckoutRequest;
import dev.harold.shopflow.order.OrderService;
import dev.harold.shopflow.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 12 clientes intentan comprar al mismo tiempo el último stock (5 unidades).
 * Sin bloqueo pesimista habría sobreventa; con SELECT ... FOR UPDATE exactamente 5 lo logran.
 */
class ConcurrencyIntegrationTest extends IntegrationTest {

    @Autowired
    OrderService orderService;

    @Test
    void neverOversellsUnderConcurrentCheckouts() throws Exception {
        Session admin = loginAdmin();
        Long productId = createProduct(admin, "999.00", 5);

        List<Session> customers = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            Session c = registerCustomer();
            addToCart(c, productId, 1);
            customers.add(c);
        }

        CheckoutRequest request = new CheckoutRequest(
                new AddressRequest("Cliente", "Jr. de la Unión 100", "Cercado", "Lima", "912345678"));
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService pool = Executors.newFixedThreadPool(customers.size())) {
            List<Future<?>> futures = new ArrayList<>();
            for (Session c : customers) {
                futures.add(pool.submit(() -> {
                    start.await();
                    try {
                        orderService.checkout(c.userId(), c.email(), request);
                        succeeded.incrementAndGet();
                    } catch (InsufficientStockException e) {
                        rejected.incrementAndGet();
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        }

        assertThat(succeeded.get()).isEqualTo(5);
        assertThat(rejected.get()).isEqualTo(7);
        assertThat(stockOf(productId)).isZero();
    }
}
