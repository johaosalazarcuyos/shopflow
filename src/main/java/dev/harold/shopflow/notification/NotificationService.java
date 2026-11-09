package dev.harold.shopflow.notification;

import dev.harold.shopflow.config.AppProperties;
import dev.harold.shopflow.order.Order;
import dev.harold.shopflow.order.OrderRepository;
import dev.harold.shopflow.order.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notifications;
    private final OrderRepository orders;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final AppProperties.Notifications config;

    public NotificationService(NotificationRepository notifications, OrderRepository orders,
                               ObjectProvider<JavaMailSender> mailSender, AppProperties props) {
        this.notifications = notifications;
        this.orders = orders;
        this.mailSender = mailSender;
        this.config = props.notifications();
    }

    @Transactional
    public void orderPlaced(Long orderId) {
        Order o = orders.findById(orderId).orElseThrow();
        send(o, "ORDER_PLACED", "Recibimos tu pedido " + o.getOrderNumber(),
                "Hola " + o.getUser().getFullName() + ",\n\nTu pedido " + o.getOrderNumber()
                        + " por S/ " + o.getTotal() + " está pendiente de pago.\n\nGracias por comprar en ShopFlow.");
    }

    @Transactional
    public void orderStatusChanged(Long orderId, OrderStatus status) {
        Order o = orders.findById(orderId).orElseThrow();
        String message = switch (status) {
            case PAID -> "¡Pago confirmado! Estamos preparando tu pedido.";
            case SHIPPED -> "Tu pedido va en camino a " + o.getShippingAddress().getDistrict() + ".";
            case DELIVERED -> "Tu pedido fue entregado. ¡Que lo disfrutes!";
            case CANCELLED -> "Tu pedido fue cancelado. Si ya habías pagado, el reembolso está en proceso.";
            case PENDING_PAYMENT -> "Tu pedido está pendiente de pago.";
        };
        send(o, "ORDER_" + status, "Pedido " + o.getOrderNumber() + ": " + status, message);
    }

    private void send(Order order, String type, String subject, String body) {
        String to = order.getUser().getEmail();
        Notification n = new Notification(to, type, subject, body);
        try {
            JavaMailSender sender = config.emailEnabled() ? mailSender.getIfAvailable() : null;
            if (sender != null) {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(config.from());
                msg.setTo(to);
                msg.setSubject(subject);
                msg.setText(body);
                sender.send(msg);
                n.markSent(true);
            } else {
                log.info("[notificación] para={} asunto='{}'", to, subject);
                n.markSent(false);
            }
        } catch (MailException e) {
            log.warn("No se pudo enviar el correo a {}: {}", to, e.getMessage());
            n.markFailed(e.getMessage());
        }
        notifications.save(n);
    }
}
