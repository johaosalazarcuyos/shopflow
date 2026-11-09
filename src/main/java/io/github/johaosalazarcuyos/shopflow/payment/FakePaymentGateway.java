package io.github.johaosalazarcuyos.shopflow.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Pasarela simulada con tokens de prueba (al estilo de las pasarelas reales):
 * tok_visa / tok_mastercard aprueban; tok_declined y tok_insufficient_funds rechazan.
 */
@Component
public class FakePaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(FakePaymentGateway.class);

    @Override
    public ChargeResult charge(ChargeRequest request) {
        ChargeResult result = switch (request.cardToken()) {
            case "tok_visa", "tok_mastercard" -> ChargeResult.ok("txn_" + UUID.randomUUID().toString().replace("-", ""));
            case "tok_declined" -> ChargeResult.declined("card_declined");
            case "tok_insufficient_funds" -> ChargeResult.declined("insufficient_funds");
            default -> ChargeResult.declined("invalid_token");
        };
        log.info("Cobro simulado {} {} ref={} -> {}", request.amount(), request.currency(), request.reference(),
                result.success() ? "APROBADO" : result.failureReason());
        return result;
    }

    @Override
    public void refund(String transactionId) {
        log.info("Reembolso simulado de {}", transactionId);
    }
}
