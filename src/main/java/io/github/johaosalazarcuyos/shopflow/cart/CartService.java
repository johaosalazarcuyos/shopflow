package io.github.johaosalazarcuyos.shopflow.cart;

import io.github.johaosalazarcuyos.shopflow.cart.CartDtos.*;
import io.github.johaosalazarcuyos.shopflow.catalog.Product;
import io.github.johaosalazarcuyos.shopflow.catalog.ProductRepository;
import io.github.johaosalazarcuyos.shopflow.common.ApiException;
import io.github.johaosalazarcuyos.shopflow.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class CartService {

    private final CartRepository carts;
    private final ProductRepository products;
    private final UserRepository users;

    public CartService(CartRepository carts, ProductRepository products, UserRepository users) {
        this.carts = carts;
        this.products = products;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public CartResponse get(Long userId) {
        return carts.findByUserId(userId).map(CartService::toResponse)
                .orElse(new CartResponse(List.of(), 0, BigDecimal.ZERO));
    }

    public CartResponse add(Long userId, AddItemRequest r) {
        Cart cart = getOrCreate(userId);
        Product product = products.findById(r.productId()).filter(Product::isActive)
                .orElseThrow(() -> ApiException.notFound("Producto " + r.productId() + " no encontrado"));
        int desired = cart.findItem(product.getId()).map(CartItem::getQuantity).orElse(0) + r.quantity();
        ensureStock(product, desired);
        cart.add(product, r.quantity());
        return toResponse(carts.save(cart));
    }

    public CartResponse update(Long userId, Long productId, UpdateItemRequest r) {
        Cart cart = getOrCreate(userId);
        CartItem item = cart.findItem(productId)
                .orElseThrow(() -> ApiException.notFound("El producto no está en el carrito"));
        if (r.quantity() == 0) {
            cart.remove(productId);
        } else {
            ensureStock(item.getProduct(), r.quantity());
            item.setQuantity(r.quantity());
        }
        return toResponse(cart);
    }

    public CartResponse remove(Long userId, Long productId) {
        Cart cart = getOrCreate(userId);
        cart.remove(productId);
        return toResponse(cart);
    }

    public void clear(Long userId) {
        carts.findByUserId(userId).ifPresent(Cart::clear);
    }

    private Cart getOrCreate(Long userId) {
        return carts.findByUserId(userId).orElseGet(() -> carts.save(new Cart(users.getReferenceById(userId))));
    }

    private static void ensureStock(Product product, int quantity) {
        if (!product.hasStock(quantity)) {
            throw ApiException.conflict("INSUFFICIENT_STOCK",
                    "Solo quedan " + product.getStock() + " unidades de " + product.getName());
        }
    }

    private static CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream().map(i -> {
            Product p = i.getProduct();
            BigDecimal line = p.getPrice().multiply(BigDecimal.valueOf(i.getQuantity()));
            return new CartItemResponse(p.getId(), p.getSku(), p.getName(), p.getPrice(), i.getQuantity(), line,
                    p.isActive() && p.hasStock(i.getQuantity()));
        }).toList();
        BigDecimal subtotal = items.stream().map(CartItemResponse::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        int count = items.stream().mapToInt(CartItemResponse::quantity).sum();
        return new CartResponse(items, count, subtotal);
    }
}
