package robert_neat.governance_mock.casesla;

/** Inbound payload for POST /cases/{caseId}/sla-check. Boxed type: a missing field must fail validation, not default to 0 (which would mean "escalate"). */
public record SlaCheckRequest(
        Double slaRemainingHours
) {
}
