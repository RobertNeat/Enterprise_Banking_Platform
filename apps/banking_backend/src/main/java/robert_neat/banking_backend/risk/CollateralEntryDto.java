package robert_neat.banking_backend.risk;

import java.math.BigDecimal;

public record CollateralEntryDto(
    String id,
    String customer,
    String type,
    BigDecimal valuation,
    BigDecimal ltv,
    String rating,
    String stage,
    BigDecimal provision) {}
