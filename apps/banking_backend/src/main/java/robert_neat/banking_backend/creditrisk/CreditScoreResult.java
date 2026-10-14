package robert_neat.banking_backend.creditrisk;

import java.math.BigDecimal;
import java.time.Instant;

/** Mirrors credit_risk_mock's POST /credit-bureau/score response (docs/credit_risk_mock_contract/api.md). */
public record CreditScoreResult(
        String applicantId,
        Integer scoreResult,
        BigDecimal dsti,
        String inquiryId,
        Instant queriedAt) {
}
