package dev.harold.shopflow.report;

import dev.harold.shopflow.order.Order;
import dev.harold.shopflow.order.OrderStatus;
import dev.harold.shopflow.report.ReportDtos.StatusCount;
import dev.harold.shopflow.report.ReportDtos.TopProduct;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

/** Consultas de agregación de solo lectura. */
public interface ReportRepository extends Repository<Order, Long> {

    @Query("""
            select coalesce(sum(o.total), 0) from CustomerOrder o
            where o.paidAt >= :from and o.paidAt < :to and o.status in :statuses""")
    BigDecimal revenue(@Param("from") Instant from, @Param("to") Instant to,
                       @Param("statuses") Collection<OrderStatus> statuses);

    @Query("""
            select count(o) from CustomerOrder o
            where o.paidAt >= :from and o.paidAt < :to and o.status in :statuses""")
    long paidOrders(@Param("from") Instant from, @Param("to") Instant to,
                    @Param("statuses") Collection<OrderStatus> statuses);

    @Query("""
            select new dev.harold.shopflow.report.ReportDtos$TopProduct(
                i.productId, i.productName, sum(i.quantity), sum(i.lineTotal))
            from OrderItem i join i.order o
            where o.paidAt >= :from and o.paidAt < :to and o.status in :statuses
            group by i.productId, i.productName
            order by sum(i.quantity) desc""")
    List<TopProduct> topProducts(@Param("from") Instant from, @Param("to") Instant to,
                                 @Param("statuses") Collection<OrderStatus> statuses, Pageable limit);

    @Query("""
            select new dev.harold.shopflow.report.ReportDtos$StatusCount(o.status, count(o))
            from CustomerOrder o where o.createdAt >= :from and o.createdAt < :to group by o.status""")
    List<StatusCount> countByStatus(@Param("from") Instant from, @Param("to") Instant to);
}
