package io.github.johaosalazarcuyos.shopflow.unit;

import io.github.johaosalazarcuyos.shopflow.catalog.Category;
import io.github.johaosalazarcuyos.shopflow.catalog.Product;
import io.github.johaosalazarcuyos.shopflow.order.*;
import io.github.johaosalazarcuyos.shopflow.user.Role;
import io.github.johaosalazarcuyos.shopflow.user.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private final User user = new User("a@b.pe", "x", "Ana", Role.CUSTOMER);
    private final Category category = new Category("Audio", "audio");

    private Order newOrder() {
        return new Order("SF-1", user, new ShippingAddress("Ana", "Calle 1", "Surco", "Lima", "999999999"), "a@b.pe");
    }

    @Test
    void itemsSnapshotPriceAndComputeTotals() {
        Product p = new Product("AUD-1", "Audífonos", null, new BigDecimal("59.90"), 10, category, null);
        Order order = newOrder();
        order.addItem(new OrderItem(order, p, 3));
        order.applyShipping(new BigDecimal("10.00"));

        p.update("Audífonos", null, new BigDecimal("99.00"), category, null); // cambio de precio posterior

        assertThat(order.getSubtotal()).isEqualByComparingTo("179.70");
        assertThat(order.getTotal()).isEqualByComparingTo("189.70");
        assertThat(order.getItems().getFirst().getUnitPrice()).isEqualByComparingTo("59.90");
    }

    @Test
    void recordsHistoryAndRejectsInvalidTransitions() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.PAID, "gateway", null);

        assertThat(order.getPaidAt()).isNotNull();
        assertThat(order.getHistory()).extracting(OrderStatusChange::getToStatus)
                .containsExactly(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID);
        assertThatThrownBy(() -> order.transitionTo(OrderStatus.DELIVERED, "admin", null))
                .isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    void productNeverGoesBelowZeroStock() {
        Product p = new Product("AUD-2", "Parlante", null, BigDecimal.TEN, 2, category, null);
        assertThatThrownBy(() -> p.decreaseStock(3)).isInstanceOf(IllegalStateException.class);
        p.decreaseStock(2);
        assertThat(p.getStock()).isZero();
    }
}
