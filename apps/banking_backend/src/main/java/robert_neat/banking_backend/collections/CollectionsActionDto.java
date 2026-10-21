package robert_neat.banking_backend.collections;

import java.time.Instant;

public record CollectionsActionDto(
    String actionType, Instant performedAt, String performedBy, String governanceAuditEventId) {}
