package io.github.johaosalazarcuyos.shopflow.catalog;

import io.github.johaosalazarcuyos.shopflow.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories")
public class Category extends BaseEntity {

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    protected Category() {
    }

    public Category(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }

    public String getName() { return name; }
    public String getSlug() { return slug; }
}
