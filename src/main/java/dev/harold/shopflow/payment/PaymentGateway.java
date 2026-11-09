package dev.harold.shopflow.payment;

import java.math.BigDecimal;

/**
 * Puerto hacia el proveedor de pagos. La app solo depende de esta interfaz,
 * así cambiar a Culqi, Mercado Pago o Stripe es implementar un adaptador nuevo.
 */
public interface PaymentGateway {

    record ChargeRequest(BigDecimal amount, String currency, String cardToken, String reference) {
    }

    record ChargeResult(boolean success, String transactionId, String failureReason) {
        public static ChargeResult ok(String transactionId) {
            return new ChargeResult(true, transactionId, null);
        }

        public static ChargeResult declined(String reason) {
            return new ChargeResult(false, null, reason);
        }
    }

    ChargeResult charge(ChargeRequest request);

    void refund(String transactionId);
}
