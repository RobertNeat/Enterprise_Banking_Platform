package robert_neat.banking_backend.loan;

import java.math.BigDecimal;

/**
 * F2 — wizard submits applicant/product/amount only; scoreResult/dsti are no longer user input,
 * they come back from credit_risk_mock (see LoanActionsService.create).
 */
public record CreateLoanApplicationRequest(
        String customerId,
        String segment,
        String product,
        BigDecimal requestedAmount,
        Integer termMonths) {
}
