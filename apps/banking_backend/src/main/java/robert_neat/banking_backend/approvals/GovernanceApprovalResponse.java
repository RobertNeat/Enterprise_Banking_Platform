package robert_neat.banking_backend.approvals;

import java.time.Instant;

/** Exact JSON shape returned by governance_mock's /approvals endpoints — deserialization-only DTO. */
public record GovernanceApprovalResponse(
        String id,
        String type,
        String requestedBy,
        String payload,
        String status,
        Instant createdAt,
        String decidedBy,
        Instant decidedAt) {

    public ApprovalRecord toApprovalRecord() {
        return new ApprovalRecord(id, type, requestedBy, payload, status, createdAt, decidedBy, decidedAt,
                null, null);
    }
}
