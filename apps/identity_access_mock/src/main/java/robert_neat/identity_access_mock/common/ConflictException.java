package robert_neat.identity_access_mock.common;

/** Thrown on duplicate unique keys (e.g. employeeId already registered); mapped to HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
