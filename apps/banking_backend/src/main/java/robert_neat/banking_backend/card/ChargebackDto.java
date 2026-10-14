package robert_neat.banking_backend.card;

import java.time.Instant;

public record ChargebackDto(
        String id,
        String cardAuthorizationId,
        String reasonCode,
        String status,
        Instant filedAt) {
}
