package robert_neat.banking_backend.loan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record LoanDetailDto(
    String id,
    String applicant,
    String segment,
    String product,
    String stage,
    BigDecimal requestedAmount,
    BigDecimal approvedAmount,
    int scoreResult,
    BigDecimal dsti,
    LocalDate createdAt,
    BigDecimal interestRate,
    int termMonths,
    List<InstallmentDto> installments,
    List<LoanStageTransitionDto> stageTransitions) {}
