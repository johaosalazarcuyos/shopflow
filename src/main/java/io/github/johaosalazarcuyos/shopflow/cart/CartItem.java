package io.github.johaosalazarcuyos.shopflow.cart;

import io.github.johaosalazarcuyos.shopflow.catalog.Product;
import io.github.johaosalazarcuyos.shopflow.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "cart_items")
public class CartItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    protected CartItem() {
    }

    CartItem(Cart cart, Product product, int quantity) {
        this.cart = cart;
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    void setQuantity(int quantity) { this.quantity = quantity; }
}
