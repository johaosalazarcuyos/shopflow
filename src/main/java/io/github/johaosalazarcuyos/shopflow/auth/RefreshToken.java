package io.github.johaosalazarcuyos.shopflow.auth;

import io.github.johaosalazarcuyos.shopflow.common.BaseEntity;
import io.github.johaosalazarcuyos.shopflow.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    /** Solo se guarda el hash SHA-256; el token en claro nunca se persiste. */
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @Column(nullable = false)
    private Instant createdAt;

    protected RefreshToken() {
    }

    public RefreshToken(User user, String tokenHash, Instant createdAt, Instant expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(Instant now) { return now.isAfter(expiresAt); }
    public void revoke() { this.revoked = true; }
    public boolean isRevoked() { return revoked; }
    public User getUser() { return user; }
}
