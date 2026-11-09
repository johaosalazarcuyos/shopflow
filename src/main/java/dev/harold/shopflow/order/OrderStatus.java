package dev.harold.shopflow.order;

import java.util.EnumSet;
import java.util.Set;

/**
 * Máquina de estados del pedido.
 * <pre>
 * PENDING_PAYMENT ──pago──▶ PAID ──▶ SHIPPED ──▶ DELIVERED
 *        │                   │
 *        └──────▶ CANCELLED ◀┘
 * </pre>
 */
public enum OrderStatus {
    PENDING_PAYMENT, PAID, SHIPPED, DELIVERED, CANCELLED;

    public Set<OrderStatus> nextStates() {
        return switch (this) {
            case PENDING_PAYMENT -> EnumSet.of(PAID, CANCELLED);
            case PAID -> EnumSet.of(SHIPPED, CANCELLED);
            case SHIPPED -> EnumSet.of(DELIVERED);
            case DELIVERED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean canTransitionTo(OrderStatus target) {
        return nextStates().contains(target);
    }

    public boolean countsAsSale() {
        return this == PAID || this == SHIPPED || this == DELIVERED;
    }
}
