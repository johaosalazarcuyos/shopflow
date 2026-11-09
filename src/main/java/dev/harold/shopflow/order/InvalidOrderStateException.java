package dev.harold.shopflow.order;

import dev.harold.shopflow.common.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidOrderStateException extends ApiException {
    public InvalidOrderStateException(OrderStatus from, OrderStatus to) {
        super(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION",
                "No se puede pasar un pedido de " + from + " a " + to);
    }
}
