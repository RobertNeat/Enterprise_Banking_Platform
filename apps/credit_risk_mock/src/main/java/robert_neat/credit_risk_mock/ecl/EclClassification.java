package robert_neat.credit_risk_mock.ecl;

import java.math.BigDecimal;
import java.time.Instant;

public record EclClassification(
        String positionId,
        String stage,
        BigDecimal provision,
        Instant classifiedAt
) {
}
