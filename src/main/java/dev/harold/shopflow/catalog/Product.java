package dev.harold.shopflow.catalog;

import dev.harold.shopflow.common.AuditedEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product extends AuditedEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String sku;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Category category;

    @Column(length = 500)
    private String imageUrl;

    /** Bloqueo optimista para ediciones concurrentes desde el panel de administración. */
    @Version
    private long version;

    protected Product() {
    }

    public Product(String sku, String name, String description, BigDecimal price, int stock,
                   Category category, String imageUrl) {
        this.sku = sku;
        update(name, description, price, category, imageUrl);
        this.stock = stock;
    }

    public void update(String name, String description, BigDecimal price, Category category, String imageUrl) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.imageUrl = imageUrl;
    }

    public boolean hasStock(int quantity) {
        return stock >= quantity;
    }

    public void decreaseStock(int quantity) {
        if (!hasStock(quantity)) {
            throw new IllegalStateException("Stock insuficiente para " + sku);
        }
        stock -= quantity;
    }

    public void increaseStock(int quantity) {
        stock += quantity;
    }

    public void setStock(int stock) { this.stock = stock; }
    public void deactivate() { this.active = false; }

    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public int getStock() { return stock; }
    public boolean isActive() { return active; }
    public Category getCategory() { return category; }
    public String getImageUrl() { return imageUrl; }
    public long getVersion() { return version; }
}
