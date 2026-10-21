package robert_neat.banking_backend.enforcement;

import java.math.BigDecimal;

public record EnforcementCaseDto(
    String id,
    String customer,
    String bailiff,
    String caseNumber,
    BigDecimal blockedAmount,
    BigDecimal transferredAmount,
    BigDecimal protectedAmount,
    String status) {}
