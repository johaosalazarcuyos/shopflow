package io.github.johaosalazarcuyos.shopflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;

/** Configuración tipada de la aplicación (prefijo {@code app.*}). */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Orders orders, Shipping shipping, Admin admin,
                            Notifications notifications, String timezone, String currency) {

    public record Jwt(String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {
    }

    public record Orders(Duration paymentTimeout, Duration expirationCheckInterval) {
    }

    public record Shipping(BigDecimal flatRate, BigDecimal freeThreshold) {
    }

    public record Admin(String email, String password) {
    }

    public record Notifications(boolean emailEnabled, String from) {
    }
}
