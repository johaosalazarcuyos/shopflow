package dev.harold.shopflow.unit;

import dev.harold.shopflow.order.OrderStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @ParameterizedTest(name = "{0} -> {1} = {2}")
    @CsvSource({
            "PENDING_PAYMENT, PAID,      true",
            "PENDING_PAYMENT, CANCELLED, true",
            "PENDING_PAYMENT, SHIPPED,   false",
            "PAID,            SHIPPED,   true",
            "PAID,            CANCELLED, true",
            "PAID,            DELIVERED, false",
            "SHIPPED,         DELIVERED, true",
            "SHIPPED,         CANCELLED, false",
            "DELIVERED,       CANCELLED, false",
            "CANCELLED,       PAID,      false"
    })
    void transitions(OrderStatus from, OrderStatus to, boolean allowed) {
        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
    }
}
