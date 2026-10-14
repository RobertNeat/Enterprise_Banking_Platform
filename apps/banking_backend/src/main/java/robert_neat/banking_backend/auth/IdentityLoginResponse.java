package robert_neat.banking_backend.auth;

import java.util.List;

/** Shape of identity_access_mock's POST /auth/login 200 response (its "token" field is ignored). */
public record IdentityLoginResponse(String token, String employeeId, String role, List<String> permissions) {
}
