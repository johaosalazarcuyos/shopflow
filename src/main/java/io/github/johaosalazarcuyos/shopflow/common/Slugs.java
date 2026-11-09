package io.github.johaosalazarcuyos.shopflow.common;

import java.text.Normalizer;
import java.util.Locale;

public final class Slugs {

    private Slugs() {
    }

    /** "Café Orgánico 500g" -> "cafe-organico-500g" */
    public static String of(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
