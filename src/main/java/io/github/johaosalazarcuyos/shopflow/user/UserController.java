package io.github.johaosalazarcuyos.shopflow.user;

import io.github.johaosalazarcuyos.shopflow.common.ApiException;
import io.github.johaosalazarcuyos.shopflow.common.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Usuarios")
public class UserController {

    public record UserResponse(Long id, String email, String fullName, Role role) {
    }

    private final UserRepository users;

    public UserController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        User u = users.findById(CurrentUser.id(jwt)).orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));
        return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole());
    }
}
