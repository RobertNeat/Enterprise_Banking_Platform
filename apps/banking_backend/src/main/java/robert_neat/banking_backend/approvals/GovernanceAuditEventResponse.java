package robert_neat.banking_backend.approvals;

import java.time.Instant;

/** Exact JSON shape returned by governance_mock's GET /audit-events — deserialization-only DTO. */
public record GovernanceAuditEventResponse(
    String id,
    String actor,
    String action,
    String entityType,
    String entityId,
    Instant timestamp,
    String details) {}
