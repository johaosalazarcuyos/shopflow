package io.github.johaosalazarcuyos.shopflow.order;

import io.github.johaosalazarcuyos.shopflow.catalog.Product;
import io.github.johaosalazarcuyos.shopflow.common.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

/** Copia (snapshot) del producto al momento de la compra: si luego cambia el precio, el pedido no cambia. */
@Entity
@Table(name = "order_items")
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Order order;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, length = 40)
    private String sku;

    @Column(nullable = false, length = 150)
    private String productName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    protected OrderItem() {
    }

    public OrderItem(Order order, Product product, int quantity) {
        this.order = order;
        this.productId = product.getId();
        this.sku = product.getSku();
        this.productName = product.getName();
        this.unitPrice = product.getPrice();
        this.quantity = quantity;
        this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getProductId() { return productId; }
    public String getSku() { return sku; }
    public String getProductName() { return productName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public BigDecimal getLineTotal() { return lineTotal; }
}
