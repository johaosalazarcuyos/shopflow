package dev.harold.shopflow.auth;

import dev.harold.shopflow.auth.AuthDtos.*;
import dev.harold.shopflow.common.ApiException;
import dev.harold.shopflow.user.Role;
import dev.harold.shopflow.user.User;
import dev.harold.shopflow.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    private final LoginAttemptLimiter limiter;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokens,
                       LoginAttemptLimiter limiter) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.limiter = limiter;
    }

    @Transactional
    public AuthResponse register(RegisterRequest r) {
        if (users.existsByEmailIgnoreCase(r.email())) {
            throw ApiException.conflict("EMAIL_TAKEN", "Ya existe una cuenta con ese email");
        }
        User user = users.save(new User(r.email().toLowerCase(), passwordEncoder.encode(r.password()),
                r.fullName(), Role.CUSTOMER));
        return tokens.issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest r) {
        limiter.checkAllowed(r.email());
        User user = users.findByEmailIgnoreCase(r.email())
                .filter(u -> passwordEncoder.matches(r.password(), u.getPasswordHash()))
                .orElse(null);
        if (user == null) {
            limiter.recordFailure(r.email());
            throw ApiException.unauthorized("BAD_CREDENTIALS", "Email o contraseña incorrectos");
        }
        limiter.reset(r.email());
        return tokens.issueTokens(user);
    }

    /** noRollbackFor: la revocación por reutilización debe persistir aunque respondamos 401. */
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse refresh(RefreshRequest r) {
        return tokens.issueTokens(tokens.consumeRefreshToken(r.refreshToken()));
    }

    @Transactional
    public void logout(RefreshRequest r) {
        tokens.revoke(r.refreshToken());
    }
}
