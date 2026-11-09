package io.github.johaosalazarcuyos.shopflow.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class CartDtos {

    private CartDtos() {
    }

    public record AddItemRequest(@NotNull Long productId, @NotNull @Min(1) @Max(99) Integer quantity) {
    }

    public record UpdateItemRequest(@NotNull @Min(0) @Max(99) Integer quantity) {
    }

    public record CartItemResponse(Long productId, String sku, String name, BigDecimal unitPrice, int quantity,
                                   BigDecimal lineTotal, boolean available) {
    }

    public record CartResponse(List<CartItemResponse> items, int itemCount, BigDecimal subtotal) {
    }
}
