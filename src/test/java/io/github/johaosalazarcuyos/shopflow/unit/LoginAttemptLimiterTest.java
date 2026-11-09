package io.github.johaosalazarcuyos.shopflow.unit;

import io.github.johaosalazarcuyos.shopflow.auth.LoginAttemptLimiter;
import io.github.johaosalazarcuyos.shopflow.common.ApiException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptLimiterTest {

    /** Reloj controlable para probar ventanas de tiempo sin esperar. */
    static final class MutableClock extends Clock {
        Instant now = Instant.parse("2026-11-08T10:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test
    void blocksAfterFiveFailuresAndUnblocksAfterWindow() {
        MutableClock clock = new MutableClock();
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(clock);

        for (int i = 0; i < 5; i++) limiter.recordFailure("Ana@Test.pe");
        assertThatThrownBy(() -> limiter.checkAllowed("ana@test.pe")).isInstanceOf(ApiException.class);

        clock.now = clock.now.plus(Duration.ofMinutes(16));
        assertThatCode(() -> limiter.checkAllowed("ana@test.pe")).doesNotThrowAnyException();
    }

    @Test
    void successfulLoginResetsCounter() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(new MutableClock());
        for (int i = 0; i < 4; i++) limiter.recordFailure("bob@test.pe");
        limiter.reset("bob@test.pe");
        limiter.recordFailure("bob@test.pe");
        assertThatCode(() -> limiter.checkAllowed("bob@test.pe")).doesNotThrowAnyException();
    }
}
