package robert_neat.governance_mock.incident;

public record CreateIncidentRequest(
        String source,
        String description,
        Severity severity
) {
}
