package io.github.johaosalazarcuyos.shopflow;

import com.jayway.jsonpath.JsonPath;
import io.github.johaosalazarcuyos.shopflow.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTest {

    @Test
    void registeredUserCanAccessProfile() throws Exception {
        Session s = registerCustomer();
        mvc.perform(get("/api/users/me").header("Authorization", s.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(s.email()))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void rejectsDuplicateEmailAndWeakPassword() throws Exception {
        Session s = registerCustomer();
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Password123","fullName":"Otra"}""".formatted(s.email())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nuevo@test.pe","password":"soloLetras","fullName":"X"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void refreshTokenRotatesAndReuseRevokesAllSessions() throws Exception {
        Session s = registerCustomer();

        String rotated = mvc.perform(refresh(s.refreshToken()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String newRefresh = JsonPath.read(rotated, "$.refreshToken");

        // Reutilizar el token viejo = posible robo -> se revoca toda la familia de sesiones
        mvc.perform(refresh(s.refreshToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSED"));
        mvc.perform(refresh(newRefresh))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSED"));
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        Session s = registerCustomer();
        mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + s.refreshToken() + "\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(refresh(s.refreshToken())).andExpect(status().isUnauthorized());
    }

    @Test
    void blocksBruteForceAfterFiveFailures() throws Exception {
        Session s = registerCustomer();
        for (int i = 0; i < 5; i++) {
            mvc.perform(login(s.email(), "Incorrecta1")).andExpect(status().isUnauthorized());
        }
        // Incluso con la contraseña correcta, queda bloqueado temporalmente
        mvc.perform(login(s.email(), "Password123")).andExpect(status().isTooManyRequests());
    }

    @Test
    void rejectsMissingOrInvalidToken() throws Exception {
        mvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/cart").header("Authorization", "Bearer token.invalido.x"))
                .andExpect(status().isUnauthorized());
    }

    private static org.springframework.test.web.servlet.RequestBuilder refresh(String token) {
        return post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + token + "\"}");
    }

    private static org.springframework.test.web.servlet.RequestBuilder login(String email, String password) {
        return post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
    }
}
