package dev.harold.shopflow.payment;

import dev.harold.shopflow.common.ApiException;
import dev.harold.shopflow.config.AppProperties;
import dev.harold.shopflow.order.Order;
import dev.harold.shopflow.order.OrderEvents.OrderStatusChanged;
import dev.harold.shopflow.order.OrderRepository;
import dev.harold.shopflow.order.OrderStatus;
import dev.harold.shopflow.payment.PaymentDtos.PaymentResponse;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository payments;
    private final OrderRepository orders;
    private final PaymentGateway gateway;
    private final ApplicationEventPublisher events;
    private final MeterRegistry meters;
    private final String currency;

    public PaymentService(PaymentRepository payments, OrderRepository orders, PaymentGateway gateway,
                          ApplicationEventPublisher events, MeterRegistry meters, AppProperties props) {
        this.payments = payments;
        this.orders = orders;
        this.gateway = gateway;
        this.events = events;
        this.meters = meters;
        this.currency = props.currency();
    }

    /**
     * Paga un pedido de forma idempotente. Si la clave ya se usó, devuelve el resultado original
     * sin volver a llamar a la pasarela. El pedido se bloquea para evitar cobros dobles concurrentes.
     */
    @Transactional
    public PaymentResponse pay(Long userId, Long orderId, String idempotencyKey, String cardToken) {
        var existing = payments.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            Payment p = existing.get();
            if (!p.getOrder().getId().equals(orderId) || !p.getOrder().getUser().getId().equals(userId)) {
                throw ApiException.conflict("IDEMPOTENCY_KEY_REUSED", "La Idempotency-Key ya se usó en otra operación");
            }
            return PaymentResponse.from(p);
        }

        Order order = orders.findForUpdate(orderId, userId)
                .orElseThrow(() -> ApiException.notFound("Pedido no encontrado"));
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw ApiException.conflict("ORDER_NOT_PAYABLE", "El pedido está en estado " + order.getStatus());
        }

        var result = gateway.charge(new PaymentGateway.ChargeRequest(order.getTotal(), currency, cardToken,
                order.getOrderNumber()));
        Payment payment = payments.save(new Payment(order, idempotencyKey, order.getTotal(), result));
        meters.counter("shopflow.payments", "status", payment.getStatus().name()).increment();

        if (result.success()) {
            order.transitionTo(OrderStatus.PAID, "payment-gateway", "Transacción " + result.transactionId());
            events.publishEvent(new OrderStatusChanged(order.getId(), OrderStatus.PAID));
        }
        return PaymentResponse.from(payment);
    }

    /** Reembolsa el pago aprobado de un pedido. Se llama dentro de la transacción de cancelación. */
    public void refund(Order order) {
        payments.findFirstByOrderIdAndStatus(order.getId(), PaymentStatus.SUCCEEDED).ifPresent(p -> {
            gateway.refund(p.getProviderTransactionId());
            p.markRefunded();
        });
    }
}
