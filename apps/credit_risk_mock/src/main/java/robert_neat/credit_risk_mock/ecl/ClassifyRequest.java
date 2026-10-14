package robert_neat.credit_risk_mock.ecl;

import java.math.BigDecimal;

public record ClassifyRequest(
        String positionId,
        int daysPastDue,
        String customerRating,
        BigDecimal exposureAmount
) {
}
