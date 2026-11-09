package io.github.johaosalazarcuyos.shopflow.payment;

import io.github.johaosalazarcuyos.shopflow.common.CurrentUser;
import io.github.johaosalazarcuyos.shopflow.payment.PaymentDtos.PayRequest;
import io.github.johaosalazarcuyos.shopflow.payment.PaymentDtos.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders/{orderId}/payments")
@Tag(name = "Pagos")
@Validated
public class PaymentController {

    private final PaymentService payments;

    public PaymentController(PaymentService payments) {
        this.payments = payments;
    }

    @PostMapping
    @Operation(summary = "Paga el pedido. Tokens de prueba: tok_visa, tok_mastercard, tok_declined, tok_insufficient_funds")
    public ResponseEntity<PaymentResponse> pay(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long orderId,
            @Parameter(description = "UUID único por intento de pago; reintentos con la misma clave no cobran dos veces")
            @RequestHeader("Idempotency-Key") @Size(min = 8, max = 100) String idempotencyKey,
            @Valid @RequestBody PayRequest request) {
        PaymentResponse response = payments.pay(CurrentUser.id(jwt), orderId, idempotencyKey, request.cardToken());
        HttpStatus status = response.status() == PaymentStatus.FAILED ? HttpStatus.PAYMENT_REQUIRED : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }
}
