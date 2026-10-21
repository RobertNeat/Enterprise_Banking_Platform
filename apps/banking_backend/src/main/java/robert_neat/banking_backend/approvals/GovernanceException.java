package robert_neat.banking_backend.approvals;

import org.springframework.http.HttpStatusCode;

/** Wraps a 4xx error response passed through from governance_mock. */
public class GovernanceException extends RuntimeException {

  private final HttpStatusCode status;

  public GovernanceException(HttpStatusCode status, String message) {
    super(message);
    this.status = status;
  }

  public HttpStatusCode getStatus() {
    return status;
  }
}
