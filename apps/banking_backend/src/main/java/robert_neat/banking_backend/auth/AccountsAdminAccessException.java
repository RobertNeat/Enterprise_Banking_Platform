package robert_neat.banking_backend.auth;

/**
 * Caller's JWT "permissions" claim lacks "accounts-admin" — thrown BEFORE forwarding to
 * identity_access_mock, which has no permission check of its own (R1.5's "approvals" permission is
 * nav-only in banking_web; this one must be enforced server-side because it assigns roles). Maps to
 * 403 — see RestExceptionHandler.
 */
public class AccountsAdminAccessException extends RuntimeException {

  public AccountsAdminAccessException(String message) {
    super(message);
  }
}
