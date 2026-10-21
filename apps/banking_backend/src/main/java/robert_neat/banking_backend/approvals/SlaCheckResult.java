package robert_neat.banking_backend.approvals;

/** Mirrors governance_mock's POST /cases/{caseId}/sla-check response. */
public record SlaCheckResult(String caseId, boolean escalated, String auditEventId) {}
