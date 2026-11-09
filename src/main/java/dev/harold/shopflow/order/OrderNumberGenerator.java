package dev.harold.shopflow.order;

import dev.harold.shopflow.config.AppProperties;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Genera números legibles: SF-20261108-7KQ2MX */
@Component
public class OrderNumberGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;
    private final ZoneId zone;

    public OrderNumberGenerator(Clock clock, AppProperties props) {
        this.clock = clock;
        this.zone = ZoneId.of(props.timezone());
    }

    public String next() {
        StringBuilder sb = new StringBuilder("SF-")
                .append(LocalDate.now(clock.withZone(zone)).format(DateTimeFormatter.BASIC_ISO_DATE)).append('-');
        for (int i = 0; i < 6; i++) sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        return sb.toString();
    }
}
