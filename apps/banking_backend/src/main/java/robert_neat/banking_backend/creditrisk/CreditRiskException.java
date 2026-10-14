package robert_neat.banking_backend.creditrisk;

import org.springframework.http.HttpStatusCode;

/** Wraps a 4xx error response passed through from credit_risk_mock. */
public class CreditRiskException extends RuntimeException {

    private final HttpStatusCode status;

    public CreditRiskException(HttpStatusCode status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatusCode getStatus() {
        return status;
    }
}
