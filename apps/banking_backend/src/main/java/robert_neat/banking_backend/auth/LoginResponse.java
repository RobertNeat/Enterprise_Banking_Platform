package robert_neat.banking_backend.auth;

import java.util.List;

/**
 * banking_backend's own login response, passed through to the UI: PL role display text and
 * permissions from identity_access_mock. The JWT's "role" CLAIM carries the stable code instead
 * (see RoleCode) — this response field is for display, the claim is for authorization logic.
 * seniorityLevel is duplicated here (also present as a JWT claim) so the frontend can gate the
 * approvals UI without decoding the JWT manually.
 */
public record LoginResponse(
        String token, String employeeId, String role, List<String> permissions, int seniorityLevel) {
}
