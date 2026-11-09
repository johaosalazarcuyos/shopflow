package io.github.johaosalazarcuyos.shopflow.order;

import io.github.johaosalazarcuyos.shopflow.common.BaseEntity;
import jakarta.persistence.*;

import java.time.Instant;

/** Historial de auditoría de cada cambio de estado. */
@Entity
@Table(name = "order_status_history")
public class OrderStatusChange extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus toStatus;

    @Column(nullable = false, length = 120)
    private String changedBy;

    @Column(length = 255)
    private String note;

    @Column(nullable = false)
    private Instant changedAt = Instant.now();

    protected OrderStatusChange() {
    }

    OrderStatusChange(Order order, OrderStatus from, OrderStatus to, String changedBy, String note) {
        this.order = order;
        this.fromStatus = from;
        this.toStatus = to;
        this.changedBy = changedBy;
        this.note = note;
    }

    public OrderStatus getFromStatus() { return fromStatus; }
    public OrderStatus getToStatus() { return toStatus; }
    public String getChangedBy() { return changedBy; }
    public String getNote() { return note; }
    public Instant getChangedAt() { return changedAt; }
}
