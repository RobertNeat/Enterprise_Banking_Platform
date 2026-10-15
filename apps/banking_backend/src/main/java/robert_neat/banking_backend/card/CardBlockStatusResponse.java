package robert_neat.banking_backend.card;

/** Mirrors card_processor_mock's POST /cards/{cardId}/block|unblock and GET block-status responses. */
public record CardBlockStatusResponse(String cardId, boolean blocked) {
}
