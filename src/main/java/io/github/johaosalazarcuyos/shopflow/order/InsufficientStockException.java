package io.github.johaosalazarcuyos.shopflow.order;

import io.github.johaosalazarcuyos.shopflow.common.ApiException;
import org.springframework.http.HttpStatus;

import java.util.List;

public class InsufficientStockException extends ApiException {

    public record Item(Long productId, String name, int requested, int available) {
    }

    private final List<Item> items;

    public InsufficientStockException(List<Item> items) {
        super(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", "Algunos productos no tienen stock suficiente");
        this.items = items;
    }

    public List<Item> getItems() { return items; }
}
