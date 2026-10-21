package robert_neat.banking_backend.auth;

/**
 * identity_access_mock returned a role with no entry in RoleCode — treated as an upstream fault
 * (502).
 */
public class UnmappedRoleException extends RuntimeException {

  public UnmappedRoleException(String role) {
    super("Unknown role returned by identity_access_mock: " + role);
  }
}
