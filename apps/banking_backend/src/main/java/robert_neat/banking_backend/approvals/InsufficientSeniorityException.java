package robert_neat.banking_backend.approvals;

/**
 * Maker-checker gate failed: either the checker's seniorityLevel is not strictly greater than the
 * requester's, or the checker is the same employee as the requester (self-approval). Maps to 403 —
 * see RestExceptionHandler. Thrown BEFORE forwarding to governance_mock, which would otherwise
 * only catch literal self-approval (via trim+equalsIgnoreCase on the id string) and not seniority.
 */
public class InsufficientSeniorityException extends RuntimeException {

    public InsufficientSeniorityException(String message) {
        super(message);
    }
}
