package dev.harold.shopflow.catalog;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record ProductRequest(
            @NotBlank @Pattern(regexp = "^[A-Z0-9-]{3,40}$", message = "solo mayúsculas, números y guiones") String sku,
            @NotBlank @Size(max = 150) String name,
            @Size(max = 5000) String description,
            @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal price,
            @NotNull @PositiveOrZero Integer stock,
            @NotNull Long categoryId,
            @Size(max = 500) String imageUrl) {
    }

    public record StockAdjustment(@NotNull Integer stock) {
    }

    public record CategoryRequest(@NotBlank @Size(max = 80) String name) {
    }

    public record CategoryResponse(Long id, String name, String slug) {
        static CategoryResponse from(Category c) {
            return new CategoryResponse(c.getId(), c.getName(), c.getSlug());
        }
    }

    public record ProductResponse(Long id, String sku, String name, String description, BigDecimal price,
                                  int stock, boolean active, Long categoryId, String categoryName, String imageUrl) {
        static ProductResponse from(Product p) {
            return new ProductResponse(p.getId(), p.getSku(), p.getName(), p.getDescription(), p.getPrice(),
                    p.getStock(), p.isActive(), p.getCategory().getId(), p.getCategory().getName(), p.getImageUrl());
        }
    }
}
