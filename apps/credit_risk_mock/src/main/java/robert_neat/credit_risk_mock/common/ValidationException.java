package robert_neat.credit_risk_mock.common;

/** Thrown by manual request validation; mapped to HTTP 400 by controllers. */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
