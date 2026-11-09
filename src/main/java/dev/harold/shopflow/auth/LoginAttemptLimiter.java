package dev.harold.shopflow.auth;

import dev.harold.shopflow.common.ApiException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita los intentos fallidos de login por cuenta (ventana deslizante) para frenar ataques de fuerza bruta.
 * En memoria: suficiente para una instancia; con varias réplicas se movería a Redis.
 */
@Component
public class LoginAttemptLimiter {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptLimiter(Clock clock) {
        this.clock = clock;
    }

    public void checkAllowed(String email) {
        Deque<Instant> attempts = failures.get(key(email));
        if (attempts == null) return;
        synchronized (attempts) {
            prune(attempts);
            if (attempts.size() >= MAX_FAILURES) {
                throw ApiException.tooManyRequests("Demasiados intentos fallidos. Intenta de nuevo en unos minutos.");
            }
        }
    }

    public void recordFailure(String email) {
        Deque<Instant> attempts = failures.computeIfAbsent(key(email), k -> new ArrayDeque<>());
        synchronized (attempts) {
            prune(attempts);
            attempts.addLast(clock.instant());
        }
    }

    public void reset(String email) {
        failures.remove(key(email));
    }

    private void prune(Deque<Instant> attempts) {
        Instant limit = clock.instant().minus(WINDOW);
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(limit)) {
            attempts.pollFirst();
        }
    }

    private static String key(String email) {
        return email.toLowerCase(Locale.ROOT);
    }
}
