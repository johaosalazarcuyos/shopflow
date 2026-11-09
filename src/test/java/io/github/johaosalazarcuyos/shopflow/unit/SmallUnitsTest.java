package io.github.johaosalazarcuyos.shopflow.unit;

import io.github.johaosalazarcuyos.shopflow.common.Slugs;
import io.github.johaosalazarcuyos.shopflow.config.AppProperties;
import io.github.johaosalazarcuyos.shopflow.order.ShippingPolicy;
import io.github.johaosalazarcuyos.shopflow.payment.FakePaymentGateway;
import io.github.johaosalazarcuyos.shopflow.payment.PaymentGateway.ChargeRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SmallUnitsTest {

    @Test
    void slugRemovesAccentsAndSymbols() {
        assertThat(Slugs.of("  Periféricos & Accesorios! ")).isEqualTo("perifericos-accesorios");
    }

    @Test
    void shippingIsFreeFromThreshold() {
        var props = new AppProperties(null, null,
                new AppProperties.Shipping(new BigDecimal("10.00"), new BigDecimal("200.00")), null, null, "UTC", "PEN");
        ShippingPolicy policy = new ShippingPolicy(props);
        assertThat(policy.costFor(new BigDecimal("199.99"))).isEqualByComparingTo("10.00");
        assertThat(policy.costFor(new BigDecimal("200.00"))).isEqualByComparingTo("0");
    }

    @Test
    void fakeGatewayBehavesLikeTestTokens() {
        FakePaymentGateway gateway = new FakePaymentGateway();
        var ok = gateway.charge(new ChargeRequest(BigDecimal.TEN, "PEN", "tok_visa", "SF-1"));
        var declined = gateway.charge(new ChargeRequest(BigDecimal.TEN, "PEN", "tok_insufficient_funds", "SF-1"));
        assertThat(ok.success()).isTrue();
        assertThat(ok.transactionId()).startsWith("txn_");
        assertThat(declined.success()).isFalse();
        assertThat(declined.failureReason()).isEqualTo("insufficient_funds");
    }
}
