package io.github.johaosalazarcuyos.shopflow.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 120) String email,
            @NotBlank @Size(min = 8, max = 100)
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "debe contener letras y números")
            String password,
            @NotBlank @Size(max = 120) String fullName) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record AuthResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
    }
}
