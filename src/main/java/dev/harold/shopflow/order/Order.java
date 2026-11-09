package dev.harold.shopflow.order;

import dev.harold.shopflow.common.AuditedEntity;
import dev.harold.shopflow.user.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Agregado raíz del pedido. "Order" es palabra reservada en SQL/JPQL, por eso el nombre de entidad. */
@Entity(name = "CustomerOrder")
@Table(name = "orders")
public class Order extends AuditedEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrderStatusChange> history = new ArrayList<>();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingCost = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Embedded
    private ShippingAddress shippingAddress;

    private Instant paidAt;

    @Version
    private long version;

    protected Order() {
    }

    public Order(String orderNumber, User user, ShippingAddress shippingAddress, String actor) {
        this.orderNumber = orderNumber;
        this.user = user;
        this.shippingAddress = shippingAddress;
        this.status = OrderStatus.PENDING_PAYMENT;
        history.add(new OrderStatusChange(this, null, OrderStatus.PENDING_PAYMENT, actor, "Pedido creado"));
    }

    public void addItem(OrderItem item) {
        items.add(item);
        subtotal = subtotal.add(item.getLineTotal());
    }

    public void applyShipping(BigDecimal cost) {
        this.shippingCost = cost;
        this.total = subtotal.add(cost);
    }

    public void transitionTo(OrderStatus target, String actor, String note) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidOrderStateException(status, target);
        }
        history.add(new OrderStatusChange(this, status, target, actor, note));
        this.status = target;
        if (target == OrderStatus.PAID) {
            this.paidAt = Instant.now();
        }
    }

    public String getOrderNumber() { return orderNumber; }
    public User getUser() { return user; }
    public OrderStatus getStatus() { return status; }
    public List<OrderItem> getItems() { return items; }
    public List<OrderStatusChange> getHistory() { return history; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getShippingCost() { return shippingCost; }
    public BigDecimal getTotal() { return total; }
    public ShippingAddress getShippingAddress() { return shippingAddress; }
    public Instant getPaidAt() { return paidAt; }
}
