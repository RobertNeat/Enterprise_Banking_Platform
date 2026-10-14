package robert_neat.governance_mock.approval;

import java.time.Instant;

public record ApprovalRequestRecord(
        String id,
        String type,
        String requestedBy,
        String payload,
        ApprovalStatus status,
        Instant createdAt,
        String decidedBy,
        Instant decidedAt
) {
    public ApprovalRequestRecord withDecision(ApprovalStatus newStatus, String decidedBy, Instant decidedAt) {
        return new ApprovalRequestRecord(id, type, requestedBy, payload, newStatus, createdAt, decidedBy, decidedAt);
    }
}
