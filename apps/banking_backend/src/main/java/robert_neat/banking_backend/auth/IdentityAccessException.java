package robert_neat.banking_backend.auth;

import org.springframework.http.HttpStatusCode;

/** Wraps a 4xx error response passed through from identity_access_mock. */
public class IdentityAccessException extends RuntimeException {

  private final HttpStatusCode status;

  public IdentityAccessException(HttpStatusCode status, String message) {
    super(message);
    this.status = status;
  }

  public HttpStatusCode getStatus() {
    return status;
  }
}
