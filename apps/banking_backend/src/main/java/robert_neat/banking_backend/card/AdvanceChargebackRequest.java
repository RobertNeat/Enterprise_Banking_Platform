package robert_neat.banking_backend.card;

/** POST /cards/chargebacks/{id}/advance request body. */
public record AdvanceChargebackRequest(String status) {}
