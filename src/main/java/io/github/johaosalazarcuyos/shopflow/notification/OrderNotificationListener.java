package io.github.johaosalazarcuyos.shopflow.notification;

import io.github.johaosalazarcuyos.shopflow.order.OrderEvents.OrderPlaced;
import io.github.johaosalazarcuyos.shopflow.order.OrderEvents.OrderStatusChanged;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Escucha eventos de pedidos solo si la transacción hizo commit (AFTER_COMMIT) y los procesa
 * en otro hilo (@Async): un fallo del correo nunca deshace ni demora la compra.
 */
@Component
public class OrderNotificationListener {

    private final NotificationService notifications;

    public OrderNotificationListener(NotificationService notifications) {
        this.notifications = notifications;
    }

    @Async
    @TransactionalEventListener
    public void on(OrderPlaced event) {
        notifications.orderPlaced(event.orderId());
    }

    @Async
    @TransactionalEventListener
    public void on(OrderStatusChanged event) {
        notifications.orderStatusChanged(event.orderId(), event.status());
    }
}
