package robert_neat.banking_backend.enforcement;

import java.math.BigDecimal;

public record TransferEnforcementRequest(BigDecimal transferredAmount) {}
