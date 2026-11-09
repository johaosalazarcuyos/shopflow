package io.github.johaosalazarcuyos.shopflow.order;

import io.github.johaosalazarcuyos.shopflow.common.CurrentUser;
import io.github.johaosalazarcuyos.shopflow.common.PageResponse;
import io.github.johaosalazarcuyos.shopflow.order.OrderDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Pedidos")
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Checkout: crea un pedido con el contenido del carrito y reserva el stock")
    public OrderResponse checkout(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CheckoutRequest request) {
        return orders.checkout(CurrentUser.id(jwt), CurrentUser.email(jwt), request);
    }

    @GetMapping
    public PageResponse<OrderSummary> list(@AuthenticationPrincipal Jwt jwt,
                                           @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return orders.listForUser(CurrentUser.id(jwt), pageable);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return orders.getForUser(CurrentUser.id(jwt), id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancela el pedido; si ya estaba pagado se reembolsa y se repone el stock")
    public OrderResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                @Valid @RequestBody(required = false) CancelRequest request) {
        return orders.cancelByCustomer(CurrentUser.id(jwt), id, CurrentUser.email(jwt),
                request == null ? null : request.reason());
    }
}
