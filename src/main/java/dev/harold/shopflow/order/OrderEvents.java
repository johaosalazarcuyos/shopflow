package dev.harold.shopflow.order;

/** Eventos de dominio publicados dentro de la transacción y procesados después del commit. */
public final class OrderEvents {

    private OrderEvents() {
    }

    public record OrderPlaced(Long orderId) {
    }

    public record OrderStatusChanged(Long orderId, OrderStatus status) {
    }
}
