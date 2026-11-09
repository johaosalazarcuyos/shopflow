package dev.harold.shopflow.report;

import dev.harold.shopflow.common.ApiException;
import dev.harold.shopflow.config.AppProperties;
import dev.harold.shopflow.order.OrderStatus;
import dev.harold.shopflow.report.ReportDtos.SalesReport;
import dev.harold.shopflow.report.ReportDtos.StatusCount;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private static final Set<OrderStatus> SALES = EnumSet.of(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

    private final ReportRepository reports;
    private final ZoneId zone;
    private final String currency;

    public ReportService(ReportRepository reports, AppProperties props) {
        this.reports = reports;
        this.zone = ZoneId.of(props.timezone());
        this.currency = props.currency();
    }

    public SalesReport sales(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw ApiException.unprocessable("INVALID_RANGE", "'to' debe ser posterior a 'from'");
        }
        // Rango [from 00:00, to+1 00:00) en hora de Lima
        Instant start = from.atStartOfDay(zone).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(zone).toInstant();

        BigDecimal revenue = reports.revenue(start, end, SALES);
        long paid = reports.paidOrders(start, end, SALES);
        BigDecimal avg = paid == 0 ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(paid), 2, RoundingMode.HALF_UP);

        Map<OrderStatus, Long> byStatus = new EnumMap<>(OrderStatus.class);
        for (OrderStatus s : OrderStatus.values()) byStatus.put(s, 0L);
        for (StatusCount c : reports.countByStatus(start, end)) byStatus.put(c.status(), c.count());

        return new SalesReport(from, to, currency, revenue, paid, avg,
                reports.topProducts(start, end, SALES, PageRequest.of(0, 5)), byStatus);
    }
}
