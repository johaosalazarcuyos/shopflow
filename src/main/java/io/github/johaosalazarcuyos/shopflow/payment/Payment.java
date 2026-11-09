package io.github.johaosalazarcuyos.shopflow.payment;

import io.github.johaosalazarcuyos.shopflow.common.BaseEntity;
import io.github.johaosalazarcuyos.shopflow.order.Order;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Order order;

    /** Clave enviada por el cliente: reintentar con la misma clave nunca cobra dos veces. */
    @Column(nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(length = 100)
    private String providerTransactionId;

    @Column(length = 255)
    private String failureReason;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Payment() {
    }

    public Payment(Order order, String idempotencyKey, BigDecimal amount, PaymentGateway.ChargeResult result) {
        this.order = order;
        this.idempotencyKey = idempotencyKey;
        this.amount = amount;
        this.status = result.success() ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED;
        this.providerTransactionId = result.transactionId();
        this.failureReason = result.failureReason();
    }

    public void markRefunded() { this.status = PaymentStatus.REFUNDED; }

    public Order getOrder() { return order; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public BigDecimal getAmount() { return amount; }
    public PaymentStatus getStatus() { return status; }
    public String getProviderTransactionId() { return providerTransactionId; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
}
