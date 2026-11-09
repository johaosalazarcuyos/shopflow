package io.github.johaosalazarcuyos.shopflow.catalog;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ProductSpecs {

    private ProductSpecs() {
    }

    static Specification<Product> search(String q, String categorySlug, BigDecimal minPrice,
                                         BigDecimal maxPrice, Boolean inStock) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.isTrue(root.get("active")));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase(Locale.ROOT) + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("description")), like)));
            }
            if (categorySlug != null) p.add(cb.equal(root.get("category").get("slug"), categorySlug));
            if (minPrice != null) p.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            if (maxPrice != null) p.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            if (Boolean.TRUE.equals(inStock)) p.add(cb.greaterThan(root.get("stock"), 0));
            return cb.and(p.toArray(Predicate[]::new));
        };
    }
}
