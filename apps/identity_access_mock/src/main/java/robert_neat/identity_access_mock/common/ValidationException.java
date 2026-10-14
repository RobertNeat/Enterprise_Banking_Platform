package robert_neat.identity_access_mock.common;

/** Thrown by manual request validation; mapped to HTTP 400. */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
