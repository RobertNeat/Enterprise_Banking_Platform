package robert_neat.banking_backend.dashboard;

import java.time.Instant;

public record IctIncidentAlertDto(
    String id, String source, String description, String severity, Instant createdAt) {}
