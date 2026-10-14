package robert_neat.banking_backend.approvals;

import java.time.Instant;

/**
 * Mirrors governance_mock's ApprovalRequestRecord JSON shape 1:1 (see
 * docs/governance_mock_contract/api.md). requestedByRole/requestedBySeniorityLevel are added by
 * banking_backend (not present upstream) so the frontend can decide decide/reject button
 * visibility without re-resolving employeeId -> role itself (single place for that mapping).
 */
public record ApprovalRecord(
        String id,
        String type,
        String requestedBy,
        String payload,
        String status,
        Instant createdAt,
        String decidedBy,
        Instant decidedAt,
        String requestedByRole,
        Integer requestedBySeniorityLevel) {

    public ApprovalRecord withRequester(String requestedByRole, Integer requestedBySeniorityLevel) {
        return new ApprovalRecord(id, type, requestedBy, payload, status, createdAt, decidedBy, decidedAt,
                requestedByRole, requestedBySeniorityLevel);
    }
}
