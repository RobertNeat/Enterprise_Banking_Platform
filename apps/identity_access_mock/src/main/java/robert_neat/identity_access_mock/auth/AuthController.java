package robert_neat.identity_access_mock.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(extractToken(authorization));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    public ResponseEntity<AccountView> registerAccount(@RequestBody RegisterRequest request) {
        EmployeeAccount account = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountView.from(account));
    }

    @GetMapping("/accounts")
    public List<AccountView> accounts() {
        return authService.listAccounts();
    }

    @PostMapping("/accounts/{id}/approve")
    public AccountView approve(@PathVariable String id, @RequestBody ApproveAccountRequest request) {
        return AccountView.from(authService.approve(id, request));
    }

    @PostMapping("/accounts/{id}/reject")
    public AccountView reject(@PathVariable String id) {
        return AccountView.from(authService.reject(id));
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring("Bearer ".length());
        }
        return authorizationHeader;
    }
}
