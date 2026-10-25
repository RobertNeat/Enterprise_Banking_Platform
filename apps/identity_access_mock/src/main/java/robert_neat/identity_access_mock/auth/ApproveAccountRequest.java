package robert_neat.identity_access_mock.auth;

/** role must be one of RolePermissions' known display names (e.g. "Doradca kredytowy"). */
public record ApproveAccountRequest(String role) {
}
