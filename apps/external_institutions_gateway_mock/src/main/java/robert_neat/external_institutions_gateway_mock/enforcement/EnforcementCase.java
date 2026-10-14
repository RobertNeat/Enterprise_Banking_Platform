package robert_neat.external_institutions_gateway_mock.enforcement;

import java.math.BigDecimal;
import java.time.Instant;

public record EnforcementCase(
        String id,
        String customer,
        String bailiff,
        String caseNumber,
        BigDecimal blockedAmount,
        BigDecimal transferredAmount,
        BigDecimal protectedAmount,
        EnforcementStatus status,
        Instant createdAt,
        Instant settledAt
) {
    public EnforcementCase withSettlement(BigDecimal transferredAmount, Instant settledAt) {
        return new EnforcementCase(id, customer, bailiff, caseNumber, blockedAmount, transferredAmount,
                protectedAmount, EnforcementStatus.SETTLED, createdAt, settledAt);
    }
}
