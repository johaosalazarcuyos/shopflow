package dev.harold.shopflow.cart;

import dev.harold.shopflow.catalog.Product;
import dev.harold.shopflow.common.AuditedEntity;
import dev.harold.shopflow.user.User;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "carts")
public class Cart extends AuditedEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<CartItem> items = new ArrayList<>();

    protected Cart() {
    }

    public Cart(User user) {
        this.user = user;
    }

    public Optional<CartItem> findItem(Long productId) {
        return items.stream().filter(i -> i.getProduct().getId().equals(productId)).findFirst();
    }

    public CartItem add(Product product, int quantity) {
        CartItem item = findItem(product.getId()).orElseGet(() -> {
            CartItem created = new CartItem(this, product, 0);
            items.add(created);
            return created;
        });
        item.setQuantity(item.getQuantity() + quantity);
        return item;
    }

    public void remove(Long productId) {
        items.removeIf(i -> i.getProduct().getId().equals(productId));
    }

    public void clear() {
        items.clear();
    }

    public boolean isEmpty() { return items.isEmpty(); }
    public List<CartItem> getItems() { return items; }
}
