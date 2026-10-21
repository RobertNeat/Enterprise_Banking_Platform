package robert_neat.banking_backend.card;

/** POST /cards/{cardId}/chargebacks request body. */
public record FileChargebackRequest(
    String authorizationId, String reasonCode, String description) {}
