package robert_neat.banking_backend.card;

import java.time.Instant;

/** Mirrors card_processor_mock's POST /chargebacks and POST /chargebacks/{id}/advance response. */
public record CardProcessorChargebackResponse(
    String id,
    String authorizationId,
    String reasonCode,
    String description,
    String status,
    Instant filedAt) {}
