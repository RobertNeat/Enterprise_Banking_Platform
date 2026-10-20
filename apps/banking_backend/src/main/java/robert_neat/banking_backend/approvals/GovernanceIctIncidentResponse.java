package robert_neat.banking_backend.approvals;

import java.time.Instant;

/** Exact JSON shape returned by governance_mock's /ict-incidents endpoints — deserialization-only DTO. */
public record GovernanceIctIncidentResponse(
        String id,
        String source,
        String description,
        String severity,
        String status,
        Instant createdAt,
        Instant resolvedAt) {
}
