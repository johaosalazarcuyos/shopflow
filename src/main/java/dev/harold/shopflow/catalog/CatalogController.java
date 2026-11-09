package dev.harold.shopflow.catalog;

import dev.harold.shopflow.catalog.CatalogDtos.CategoryResponse;
import dev.harold.shopflow.catalog.CatalogDtos.ProductResponse;
import dev.harold.shopflow.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Catálogo (público)")
@SecurityRequirements
public class CatalogController {

    private final ProductService products;
    private final CategoryRepository categories;

    public CatalogController(ProductService products, CategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    @GetMapping("/products")
    @Operation(summary = "Busca productos: texto, categoría, rango de precio y disponibilidad")
    public PageResponse<ProductResponse> search(@RequestParam(required = false) String q,
                                                @RequestParam(required = false) String category,
                                                @RequestParam(required = false) BigDecimal minPrice,
                                                @RequestParam(required = false) BigDecimal maxPrice,
                                                @RequestParam(required = false) Boolean inStock,
                                                @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return products.search(q, category, minPrice, maxPrice, inStock, pageable);
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "Detalle de producto (cacheado con Caffeine)")
    public ProductResponse get(@PathVariable Long id) {
        return products.getPublic(id);
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return categories.findAll(Sort.by("name")).stream().map(CategoryResponse::from).toList();
    }
}
