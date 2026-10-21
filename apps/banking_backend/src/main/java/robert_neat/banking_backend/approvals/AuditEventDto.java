package robert_neat.banking_backend.approvals;

/**
 * Public shape of GET /audit-events — timestamp is pre-formatted to Europe/Warsaw local time (same
 * convention as EbankingUserDto.lastLogin, see common/BankTimeZone) so the frontend can render it
 * directly without its own Instant/timezone handling.
 */
public record AuditEventDto(
    String id,
    String actor,
    String action,
    String entityType,
    String entityId,
    String timestamp,
    String details) {}
