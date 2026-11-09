package dev.harold.shopflow.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record AddressRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Size(max = 200) String line1,
            @NotBlank @Size(max = 80) String district,
            @NotBlank @Size(max = 80) String city,
            @NotBlank @Pattern(regexp = "^9\\d{8}$", message = "debe ser un celular peruano de 9 dígitos") String phone) {
    }

    public record CheckoutRequest(@NotNull @Valid AddressRequest shippingAddress) {
    }

    public record StatusUpdateRequest(@NotNull OrderStatus status, @Size(max = 255) String note) {
    }

    public record CancelRequest(@Size(max = 255) String reason) {
    }

    public record OrderItemResponse(Long productId, String sku, String name, BigDecimal unitPrice, int quantity,
                                    BigDecimal lineTotal) {
    }

    public record StatusChangeResponse(OrderStatus from, OrderStatus to, String changedBy, String note, Instant at) {
    }

    public record OrderSummary(Long id, String orderNumber, OrderStatus status, BigDecimal total, Instant createdAt) {
        static OrderSummary from(Order o) {
            return new OrderSummary(o.getId(), o.getOrderNumber(), o.getStatus(), o.getTotal(), o.getCreatedAt());
        }
    }

    public record OrderResponse(Long id, String orderNumber, OrderStatus status, List<OrderItemResponse> items,
                                BigDecimal subtotal, BigDecimal shippingCost, BigDecimal total,
                                AddressRequest shippingAddress, Instant createdAt, Instant paidAt,
                                List<StatusChangeResponse> history) {
        static OrderResponse from(Order o) {
            ShippingAddress a = o.getShippingAddress();
            return new OrderResponse(o.getId(), o.getOrderNumber(), o.getStatus(),
                    o.getItems().stream().map(i -> new OrderItemResponse(i.getProductId(), i.getSku(),
                            i.getProductName(), i.getUnitPrice(), i.getQuantity(), i.getLineTotal())).toList(),
                    o.getSubtotal(), o.getShippingCost(), o.getTotal(),
                    new AddressRequest(a.getFullName(), a.getLine1(), a.getDistrict(), a.getCity(), a.getPhone()),
                    o.getCreatedAt(), o.getPaidAt(),
                    o.getHistory().stream().map(h -> new StatusChangeResponse(h.getFromStatus(), h.getToStatus(),
                            h.getChangedBy(), h.getNote(), h.getChangedAt())).toList());
        }
    }
}
