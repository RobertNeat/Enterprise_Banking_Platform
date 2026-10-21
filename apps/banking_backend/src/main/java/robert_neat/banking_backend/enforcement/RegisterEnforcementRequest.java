package robert_neat.banking_backend.enforcement;

import java.math.BigDecimal;

/**
 * POST /enforcement body — no transferredAmount: a new case is always 'registered' with nothing
 * transferred yet. kind ("ALIMONY"/"NON_ALIMONY") is optional on the wire and defaults to
 * NON_ALIMONY in EnforcementActionsService.register() so existing callers that omit it keep
 * working.
 */
public record RegisterEnforcementRequest(
    String bailiff,
    String caseNumber,
    String customer,
    String kind,
    BigDecimal blockedAmount,
    BigDecimal protectedAmount) {}
