package robert_neat.external_institutions_gateway_mock.enforcement;

import java.time.Instant;

public record SettlementConfirmation(String id, EnforcementStatus status, Instant confirmedAt) {
}
