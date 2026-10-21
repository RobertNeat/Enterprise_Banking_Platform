package robert_neat.banking_backend.auth;

/** Thrown when identity_access_mock cannot be reached at all (connection failure, timeout). */
public class IdentityAccessUnavailableException extends RuntimeException {

  public IdentityAccessUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
