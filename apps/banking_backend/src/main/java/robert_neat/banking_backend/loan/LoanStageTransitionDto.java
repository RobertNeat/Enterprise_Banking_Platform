package robert_neat.banking_backend.loan;

import java.time.Instant;

public record LoanStageTransitionDto(
    String fromStage,
    String toStage,
    Instant transitionedAt,
    String transitionedBy,
    String approvalId) {}
