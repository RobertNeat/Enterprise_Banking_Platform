package robert_neat.credit_risk_mock.common;

/** Thrown when a requested in-memory record does not exist; mapped to HTTP 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
