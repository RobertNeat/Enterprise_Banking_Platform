package robert_neat.banking_backend.approvals;

/**
 * Request body accepted from banking_web for POST /approvals/{id}/decide. decidedBy is NOT part of
 * this body on purpose — the backend sets it from the caller's JWT subject, never trusting a
 * client-supplied identity for the maker-checker decision.
 */
public record DecideApprovalRequest(String decision) {}
