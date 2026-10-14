package robert_neat.governance_mock.approval;

public record DecideRequest(
        Decision decision,
        String decidedBy
) {
}
