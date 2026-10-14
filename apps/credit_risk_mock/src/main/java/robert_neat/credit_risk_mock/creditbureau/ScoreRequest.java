package robert_neat.credit_risk_mock.creditbureau;

import java.math.BigDecimal;

public record ScoreRequest(String applicantId, BigDecimal requestedAmount, String segment) {
}
