package io.github.johaosalazarcuyos.shopflow.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsBySlug(String slug);
}
