package io.github.johaosalazarcuyos.shopflow.catalog;

import io.github.johaosalazarcuyos.shopflow.catalog.CatalogDtos.ProductRequest;
import io.github.johaosalazarcuyos.shopflow.catalog.CatalogDtos.ProductResponse;
import io.github.johaosalazarcuyos.shopflow.common.ApiException;
import io.github.johaosalazarcuyos.shopflow.common.PageResponse;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Objects;

@Service
public class ProductService {

    public static final String CACHE = "products";

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final CacheManager cacheManager;

    public ProductService(ProductRepository products, CategoryRepository categories, CacheManager cacheManager) {
        this.products = products;
        this.categories = categories;
        this.cacheManager = cacheManager;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String q, String category, BigDecimal minPrice, BigDecimal maxPrice,
                                                Boolean inStock, Pageable pageable) {
        return PageResponse.of(products.findAll(ProductSpecs.search(q, category, minPrice, maxPrice, inStock), pageable)
                .map(ProductResponse::from));
    }

    @Cacheable(CACHE)
    @Transactional(readOnly = true)
    public ProductResponse getPublic(Long id) {
        return products.findById(id).filter(Product::isActive).map(ProductResponse::from)
                .orElseThrow(() -> ApiException.notFound("Producto " + id + " no encontrado"));
    }

    @Transactional
    public ProductResponse create(ProductRequest r) {
        if (products.existsBySku(r.sku())) {
            throw ApiException.conflict("SKU_TAKEN", "Ya existe un producto con SKU " + r.sku());
        }
        Product p = new Product(r.sku(), r.name(), r.description(), r.price(), r.stock(), category(r.categoryId()), r.imageUrl());
        return ProductResponse.from(products.save(p));
    }

    @CacheEvict(cacheNames = CACHE, key = "#id")
    @Transactional
    public ProductResponse update(Long id, ProductRequest r) {
        Product p = find(id);
        if (!p.getSku().equals(r.sku())) {
            throw ApiException.unprocessable("SKU_IMMUTABLE", "El SKU no se puede modificar");
        }
        p.update(r.name(), r.description(), r.price(), category(r.categoryId()), r.imageUrl());
        p.setStock(r.stock());
        return ProductResponse.from(products.saveAndFlush(p));
    }

    @CacheEvict(cacheNames = CACHE, key = "#id")
    @Transactional
    public ProductResponse setStock(Long id, int stock) {
        if (stock < 0) throw ApiException.unprocessable("INVALID_STOCK", "El stock no puede ser negativo");
        Product p = find(id);
        p.setStock(stock);
        return ProductResponse.from(products.saveAndFlush(p));
    }

    @CacheEvict(cacheNames = CACHE, key = "#id")
    @Transactional
    public void deactivate(Long id) {
        find(id).deactivate();
    }

    /** Usado por pedidos al mover stock, para no servir stock desactualizado desde la caché. */
    public void evict(Collection<Long> ids) {
        var cache = Objects.requireNonNull(cacheManager.getCache(CACHE));
        ids.forEach(cache::evict);
    }

    private Product find(Long id) {
        return products.findById(id).orElseThrow(() -> ApiException.notFound("Producto " + id + " no encontrado"));
    }

    private Category category(Long id) {
        return categories.findById(id).orElseThrow(() -> ApiException.notFound("Categoría " + id + " no encontrada"));
    }
}
