package dev.harold.shopflow.common;

import org.springframework.security.oauth2.jwt.Jwt;

public final class CurrentUser {

    private CurrentUser() {
    }

    /** El subject del access token es el id del usuario. */
    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }

    public static String email(Jwt jwt) {
        return jwt.getClaimAsString("email");
    }
}
