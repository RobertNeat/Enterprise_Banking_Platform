package robert_neat.banking_backend.loan;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanSummaryDto(
        String id,
        String applicant,
        String segment,
        String product,
        String stage,
        BigDecimal requestedAmount,
        BigDecimal approvedAmount,
        int scoreResult,
        BigDecimal dsti,
        LocalDate createdAt) {
}
