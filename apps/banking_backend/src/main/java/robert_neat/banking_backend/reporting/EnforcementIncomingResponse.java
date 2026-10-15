package robert_neat.banking_backend.reporting;

import java.math.BigDecimal;
import java.time.Instant;

/** Mirrors external_institutions_gateway_mock's POST /enforcement/incoming response shape. */
public record EnforcementIncomingResponse(
        String id,
        String customer,
        String bailiff,
        String caseNumber,
        BigDecimal blockedAmount,
        BigDecimal transferredAmount,
        BigDecimal protectedAmount,
        String status,
        Instant createdAt,
        Instant settledAt) {
}
