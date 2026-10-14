package robert_neat.external_institutions_gateway_mock.enforcement;

import java.math.BigDecimal;

public record IncomingEnforcementRequest(
        String bailiff,
        String caseNumber,
        String customer,
        BigDecimal blockedAmount,
        BigDecimal protectedAmount
) {
}
