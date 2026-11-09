package io.github.johaosalazarcuyos.shopflow.auth;

import io.github.johaosalazarcuyos.shopflow.common.ApiException;
import io.github.johaosalazarcuyos.shopflow.config.AppProperties;
import io.github.johaosalazarcuyos.shopflow.user.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

/**
 * Emite access tokens JWT de corta duración y refresh tokens opacos con rotación.
 * Si se reutiliza un refresh token ya rotado, se revocan todas las sesiones del usuario
 * (detección de robo de token).
 */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final RefreshTokenRepository refreshTokens;
    private final AppProperties.Jwt config;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public TokenService(JwtEncoder encoder, RefreshTokenRepository refreshTokens, AppProperties props, Clock clock) {
        this.encoder = encoder;
        this.refreshTokens = refreshTokens;
        this.config = props.jwt();
        this.clock = clock;
    }

    /** Debe llamarse dentro de una transacción. */
    public AuthDtos.AuthResponse issueTokens(User user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("shopflow")
                .issuedAt(now)
                .expiresAt(now.plus(config.accessTokenTtl()))
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", List.of(user.getRole().name()))
                .build();
        String accessToken = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokens.save(new RefreshToken(user, sha256(refreshToken), now, now.plus(config.refreshTokenTtl())));

        return new AuthDtos.AuthResponse(accessToken, refreshToken, "Bearer", config.accessTokenTtl().toSeconds());
    }

    /** Valida y revoca el refresh token; devuelve su usuario. Debe llamarse dentro de una transacción. */
    public User consumeRefreshToken(String rawToken) {
        RefreshToken token = refreshTokens.findByTokenHash(sha256(rawToken))
                .orElseThrow(() -> ApiException.unauthorized("INVALID_REFRESH_TOKEN", "Refresh token inválido"));
        if (token.isRevoked()) {
            refreshTokens.revokeAllForUser(token.getUser().getId());
            throw ApiException.unauthorized("REFRESH_TOKEN_REUSED",
                    "Refresh token reutilizado: se cerraron todas las sesiones por seguridad");
        }
        if (token.isExpired(clock.instant())) {
            throw ApiException.unauthorized("REFRESH_TOKEN_EXPIRED", "Refresh token expirado");
        }
        token.revoke();
        return token.getUser();
    }

    public void revoke(String rawToken) {
        refreshTokens.findByTokenHash(sha256(rawToken)).ifPresent(RefreshToken::revoke);
    }

    static String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
