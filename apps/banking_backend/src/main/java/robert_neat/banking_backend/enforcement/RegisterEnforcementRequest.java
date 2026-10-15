package robert_neat.banking_backend.enforcement;

import java.math.BigDecimal;

/** POST /enforcement body — no transferredAmount: a new case is always 'registered' with nothing transferred yet. */
public record RegisterEnforcementRequest(
        String bailiff, String caseNumber, String customer, BigDecimal blockedAmount, BigDecimal protectedAmount) {
}
