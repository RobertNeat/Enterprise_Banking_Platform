package robert_neat.governance_mock.audit;

public record AuditEventRequest(
        String actor,
        String action,
        String entityType,
        String entityId,
        String details
) {
}
