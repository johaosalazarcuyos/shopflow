package io.github.johaosalazarcuyos.shopflow;

import io.github.johaosalazarcuyos.shopflow.order.ExpiredOrderJob;
import io.github.johaosalazarcuyos.shopflow.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class ExpiredOrderJobIntegrationTest extends IntegrationTest {

    @Autowired
    ExpiredOrderJob job;

    @Test
    void cancelsUnpaidOrdersAfterTimeoutAndReleasesStock() throws Exception {
        Session admin = loginAdmin();
        Session customer = registerCustomer();
        Long productId = createProduct(admin, "40.00", 4);
        addToCart(customer, productId, 4);
        Long expired = checkout(customer);

        addToCart(customer, createProduct(admin, "40.00", 4), 1);
        Long fresh = checkout(customer);

        jdbc.update("update orders set created_at = now() - interval '2 hours' where id = ?", expired);
        assertThat(stockOf(productId)).isZero();

        job.run();

        assertThat(statusOf(expired)).isEqualTo("CANCELLED");
        assertThat(statusOf(fresh)).isEqualTo("PENDING_PAYMENT");
        assertThat(stockOf(productId)).isEqualTo(4);
        assertThat(jdbc.queryForObject(
                "select changed_by from order_status_history where order_id = ? and to_status = 'CANCELLED'",
                String.class, expired)).isEqualTo("system");
    }

    private String statusOf(Long orderId) {
        return jdbc.queryForObject("select status from orders where id = ?", String.class, orderId);
    }
}
