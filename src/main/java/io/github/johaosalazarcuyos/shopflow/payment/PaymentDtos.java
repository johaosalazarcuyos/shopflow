package io.github.johaosalazarcuyos.shopflow.payment;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;

public final class PaymentDtos {

    private PaymentDtos() {
    }

    public record PayRequest(@NotBlank String cardToken) {
    }

    public record PaymentResponse(Long id, Long orderId, BigDecimal amount, PaymentStatus status,
                                  String transactionId, String failureReason, Instant createdAt) {
        static PaymentResponse from(Payment p) {
            return new PaymentResponse(p.getId(), p.getOrder().getId(), p.getAmount(), p.getStatus(),
                    p.getProviderTransactionId(), p.getFailureReason(), p.getCreatedAt());
        }
    }
}
