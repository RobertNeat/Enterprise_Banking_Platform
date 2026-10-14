package robert_neat.governance_mock.casesla;

public record SlaCheckResponse(
        String caseId,
        boolean escalated,
        String auditEventId
) {
}
