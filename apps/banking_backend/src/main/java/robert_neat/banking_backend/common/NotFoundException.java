package robert_neat.banking_backend.common;

/** Thrown when a requested record does not exist; mapped to HTTP 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
