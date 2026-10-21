package robert_neat.banking_backend.reporting;

import org.springframework.http.HttpStatusCode;

/** Wraps a 4xx error response passed through from external_institutions_gateway_mock. */
public class RegulatoryGatewayException extends RuntimeException {

  private final HttpStatusCode status;

  public RegulatoryGatewayException(HttpStatusCode status, String message) {
    super(message);
    this.status = status;
  }

  public HttpStatusCode getStatus() {
    return status;
  }
}
