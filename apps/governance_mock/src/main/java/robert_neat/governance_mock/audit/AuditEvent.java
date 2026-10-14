package robert_neat.governance_mock.audit;

import java.time.Instant;

public record AuditEvent(
        String id,
        String actor,
        String action,
        String entityType,
        String entityId,
        Instant timestamp,
        String details
) {
}
