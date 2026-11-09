package dev.harold.shopflow.support;

import com.jayway.jsonpath.JsonPath;
import dev.harold.shopflow.TestcontainersConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de los tests de integración: levanta la app completa contra PostgreSQL en Testcontainers.
 * El contexto (y el contenedor) se reutiliza entre clases gracias al cache de contextos de Spring.
 */
@SpringBootTest(properties = "app.orders.expiration-check-interval=PT1H")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    protected static final String ADDRESS = """
            {"shippingAddress":{"fullName":"Ana Torres","line1":"Av. Arequipa 1234, Dpto 501",
             "district":"Miraflores","city":"Lima","phone":"987654321"}}""";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    public record Session(Long userId, String email, String accessToken, String refreshToken) {
        public String bearer() {
            return "Bearer " + accessToken;
        }
    }

    protected static String unique(String prefix) {
        return prefix + "-" + SEQ.incrementAndGet() + "-" + UUID.randomUUID().toString().substring(0, 6);
    }

    protected Session registerCustomer() throws Exception {
        String email = unique("cliente").toLowerCase() + "@test.pe";
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Password123","fullName":"Cliente de Prueba"}""".formatted(email)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return session(email, body);
    }

    protected Session loginAdmin() throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@shopflow.dev","password":"Admin12345"}"""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return session("admin@shopflow.dev", body);
    }

    private Session session(String email, String authBody) throws Exception {
        String access = JsonPath.read(authBody, "$.accessToken");
        String refresh = JsonPath.read(authBody, "$.refreshToken");
        String me = mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + access))
                .andReturn().getResponse().getContentAsString();
        return new Session(longAt(me, "$.id"), email, access, refresh);
    }

    protected Long createProduct(Session admin, String price, int stock) throws Exception {
        Long categoryId = jdbc.queryForObject("select id from categories where slug = 'perifericos'", Long.class);
        String sku = unique("TEST").toUpperCase();
        String body = mvc.perform(post("/api/admin/products").header("Authorization", admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"%s","name":"Producto %s","description":"Producto de prueba","price":%s,
                                 "stock":%d,"categoryId":%d}""".formatted(sku, sku, price, stock, categoryId)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return longAt(body, "$.id");
    }

    protected ResultActions addToCart(Session s, Long productId, int quantity) throws Exception {
        return mvc.perform(post("/api/cart/items").header("Authorization", s.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"productId":%d,"quantity":%d}""".formatted(productId, quantity)));
    }

    protected ResultActions checkoutRequest(Session s) throws Exception {
        return mvc.perform(post("/api/orders").header("Authorization", s.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(ADDRESS));
    }

    protected Long checkout(Session s) throws Exception {
        String body = checkoutRequest(s).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return longAt(body, "$.id");
    }

    protected ResultActions pay(Session s, Long orderId, String idempotencyKey, String cardToken) throws Exception {
        return mvc.perform(post("/api/orders/{id}/payments", orderId).header("Authorization", s.bearer())
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"cardToken":"%s"}""".formatted(cardToken)));
    }

    protected ResultActions setStatus(Session admin, Long orderId, String status) throws Exception {
        return mvc.perform(patch("/api/admin/orders/{id}/status", orderId).header("Authorization", admin.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"%s","note":"test"}""".formatted(status)));
    }

    protected int stockOf(Long productId) {
        return jdbc.queryForObject("select stock from products where id = ?", Integer.class, productId);
    }

    protected static Long longAt(String json, String path) {
        return ((Number) JsonPath.read(json, path)).longValue();
    }

    protected static void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) throw new AssertionError("La condición no se cumplió a tiempo");
            Thread.sleep(50);
        }
    }
}
