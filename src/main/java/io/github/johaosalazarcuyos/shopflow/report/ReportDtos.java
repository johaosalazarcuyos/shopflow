package io.github.johaosalazarcuyos.shopflow.report;

import io.github.johaosalazarcuyos.shopflow.order.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class ReportDtos {

    private ReportDtos() {
    }

    public record TopProduct(Long productId, String name, Long unitsSold, BigDecimal revenue) {
    }

    public record StatusCount(OrderStatus status, Long count) {
    }

    public record SalesReport(LocalDate from, LocalDate to, String currency, BigDecimal revenue, long paidOrders,
                              BigDecimal averageOrderValue, List<TopProduct> topProducts,
                              Map<OrderStatus, Long> ordersByStatus) {
    }
}
