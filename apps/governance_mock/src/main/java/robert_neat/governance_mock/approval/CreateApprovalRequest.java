package robert_neat.governance_mock.approval;

public record CreateApprovalRequest(
        String type,
        String requestedBy,
        String payload
) {
}
