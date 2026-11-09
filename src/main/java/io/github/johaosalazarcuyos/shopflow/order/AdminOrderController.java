package io.github.johaosalazarcuyos.shopflow.order;

import io.github.johaosalazarcuyos.shopflow.common.CurrentUser;
import io.github.johaosalazarcuyos.shopflow.common.PageResponse;
import io.github.johaosalazarcuyos.shopflow.order.OrderDtos.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@Tag(name = "Admin · Pedidos")
public class AdminOrderController {

    private final OrderService orders;

    public AdminOrderController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping
    public PageResponse<OrderSummary> list(@RequestParam(required = false) OrderStatus status,
                                           @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return orders.listAll(status, pageable);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return orders.getAny(id);
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                      @Valid @RequestBody StatusUpdateRequest request) {
        return orders.updateStatus(id, request, CurrentUser.email(jwt));
    }
}
