package robert_neat.banking_backend.auth;

import tools.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** /auth/login issues banking_backend's own JWT; /auth/logout and /auth/register proxy to identity_access_mock. */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final IdentityAccessClient identityAccessClient;

    public AuthController(AuthService authService, IdentityAccessClient identityAccessClient) {
        this.authService = authService;
        this.identityAccessClient = identityAccessClient;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        authService.logout(jwt.getTokenValue());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    public ResponseEntity<JsonNode> register(@RequestBody JsonNode request) {
        JsonNode response = identityAccessClient.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
