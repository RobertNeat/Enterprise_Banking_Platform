package robert_neat.banking_backend.approvals;

/** Thrown when governance_mock cannot be reached at all (connection failure, timeout). */
public class GovernanceUnavailableException extends RuntimeException {

    public GovernanceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
