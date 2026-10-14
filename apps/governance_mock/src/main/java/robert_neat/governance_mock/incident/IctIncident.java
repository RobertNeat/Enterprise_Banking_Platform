package robert_neat.governance_mock.incident;

import java.time.Instant;

public record IctIncident(
        String id,
        String source,
        String description,
        Severity severity,
        IncidentStatus status,
        Instant createdAt,
        Instant resolvedAt
) {
    public IctIncident resolved(Instant resolvedAt) {
        return new IctIncident(id, source, description, severity, IncidentStatus.RESOLVED, createdAt, resolvedAt);
    }
}
