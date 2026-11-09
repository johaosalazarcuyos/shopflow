package dev.harold.shopflow.catalog;

import dev.harold.shopflow.catalog.CatalogDtos.*;
import dev.harold.shopflow.common.ApiException;
import dev.harold.shopflow.common.Slugs;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin · Catálogo")
public class AdminCatalogController {

    private final ProductService products;
    private final CategoryRepository categories;

    public AdminCatalogController(ProductService products, CategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return products.create(request);
    }

    @PutMapping("/products/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return products.update(id, request);
    }

    @PatchMapping("/products/{id}/stock")
    public ProductResponse setStock(@PathVariable Long id, @Valid @RequestBody StockAdjustment request) {
        return products.setStock(id, request.stock());
    }

    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        products.deactivate(id);
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request) {
        String slug = Slugs.of(request.name());
        if (categories.existsBySlug(slug)) {
            throw ApiException.conflict("CATEGORY_EXISTS", "La categoría ya existe");
        }
        return CategoryResponse.from(categories.save(new Category(request.name(), slug)));
    }
}
