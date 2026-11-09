package io.github.johaosalazarcuyos.shopflow.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Libera el stock reservado por pedidos que nunca se pagaron. */
@Component
public class ExpiredOrderJob {

    private static final Logger log = LoggerFactory.getLogger(ExpiredOrderJob.class);

    private final OrderService orders;

    public ExpiredOrderJob(OrderService orders) {
        this.orders = orders;
    }

    @Scheduled(fixedDelayString = "${app.orders.expiration-check-interval}",
            initialDelayString = "${app.orders.expiration-check-interval}")
    public void run() {
        int cancelled = orders.cancelExpiredOrders();
        if (cancelled > 0) {
            log.info("Pedidos vencidos cancelados: {}", cancelled);
        }
    }
}
