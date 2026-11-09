package io.github.johaosalazarcuyos.shopflow.order;

import io.github.johaosalazarcuyos.shopflow.config.AppProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Envío gratis desde cierto monto; si no, tarifa plana. */
@Component
public class ShippingPolicy {

    private final AppProperties.Shipping config;

    public ShippingPolicy(AppProperties props) {
        this.config = props.shipping();
    }

    public BigDecimal costFor(BigDecimal subtotal) {
        return subtotal.compareTo(config.freeThreshold()) >= 0 ? BigDecimal.ZERO : config.flatRate();
    }
}
