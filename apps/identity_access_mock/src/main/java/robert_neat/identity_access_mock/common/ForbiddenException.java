package robert_neat.identity_access_mock.common;

/** Thrown when an action requires a precondition that was not met (e.g. verified OTP); mapped to HTTP 403. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
