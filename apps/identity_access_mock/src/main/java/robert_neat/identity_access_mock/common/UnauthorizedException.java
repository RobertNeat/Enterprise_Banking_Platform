package robert_neat.identity_access_mock.common;

/** Thrown when credentials are invalid or a token is missing/unknown; mapped to HTTP 401. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
