package robert_neat.credit_risk_mock.creditbureau;

import java.math.BigDecimal;
import java.time.Instant;

public record CreditBureauInquiry(
        String applicantId,
        int scoreResult,
        BigDecimal dsti,
        String inquiryId,
        Instant queriedAt
) {
}
