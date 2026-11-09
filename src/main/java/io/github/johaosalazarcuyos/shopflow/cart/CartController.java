package io.github.johaosalazarcuyos.shopflow.cart;

import io.github.johaosalazarcuyos.shopflow.cart.CartDtos.*;
import io.github.johaosalazarcuyos.shopflow.common.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@Tag(name = "Carrito")
public class CartController {

    private final CartService carts;

    public CartController(CartService carts) {
        this.carts = carts;
    }

    @GetMapping
    public CartResponse get(@AuthenticationPrincipal Jwt jwt) {
        return carts.get(CurrentUser.id(jwt));
    }

    @PostMapping("/items")
    public CartResponse add(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddItemRequest request) {
        return carts.add(CurrentUser.id(jwt), request);
    }

    @PutMapping("/items/{productId}")
    public CartResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId,
                               @Valid @RequestBody UpdateItemRequest request) {
        return carts.update(CurrentUser.id(jwt), productId, request);
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId) {
        return carts.remove(CurrentUser.id(jwt), productId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@AuthenticationPrincipal Jwt jwt) {
        carts.clear(CurrentUser.id(jwt));
    }
}
