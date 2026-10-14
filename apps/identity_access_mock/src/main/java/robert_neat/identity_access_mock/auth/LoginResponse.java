package robert_neat.identity_access_mock.auth;

import java.util.List;

public record LoginResponse(String token, String employeeId, String role, List<String> permissions) {
}
