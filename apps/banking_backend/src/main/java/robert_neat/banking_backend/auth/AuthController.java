package robert_neat.banking_backend.auth;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/**
 * /auth/login issues banking_backend's own JWT; /auth/logout and /auth/register proxy to
 * identity_access_mock. The /auth/accounts* admin endpoints additionally require the
 * "accounts-admin" permission claim — identity_access_mock has no permission check of its own.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

  private static final String ACCOUNTS_ADMIN_PERMISSION = "accounts-admin";

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

  @GetMapping("/accounts")
  public List<JsonNode> accounts(@AuthenticationPrincipal Jwt jwt) {
    requireAccountsAdmin(jwt);
    return identityAccessClient.listAccounts();
  }

  @PostMapping("/accounts/{id}/approve")
  public JsonNode approveAccount(
      @PathVariable String id, @RequestBody JsonNode request, @AuthenticationPrincipal Jwt jwt) {
    requireAccountsAdmin(jwt);
    return identityAccessClient.approveAccount(id, request);
  }

  @PostMapping("/accounts/{id}/reject")
  public JsonNode rejectAccount(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
    requireAccountsAdmin(jwt);
    return identityAccessClient.rejectAccount(id);
  }

  private void requireAccountsAdmin(Jwt jwt) {
    List<String> permissions = jwt.getClaimAsStringList("permissions");
    if (permissions == null || !permissions.contains(ACCOUNTS_ADMIN_PERMISSION)) {
      throw new AccountsAdminAccessException("Missing permission: " + ACCOUNTS_ADMIN_PERMISSION);
    }
  }
}
