package robert_neat.banking_backend.card;

import java.math.BigDecimal;
import java.time.Instant;

/** Mirrors card_processor_mock's POST/GET /authorizations response shape. */
public record CardProcessorAuthorizationResponse(
        String id,
        String cardId,
        String merchant,
        String mcc,
        BigDecimal amount,
        Instant date,
        String processor,
        boolean approved,
        String responseCode,
        String processorId) {
}
