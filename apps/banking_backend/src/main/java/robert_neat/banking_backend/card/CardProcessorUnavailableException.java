package robert_neat.banking_backend.card;

/**
 * Thrown when card_processor_mock cannot fulfil the request because it is unavailable — either a
 * real connection failure (refused/timeout) OR the processor's own simulated outage (HTTP 503
 * returned by POST /authorizations when POST /processor/simulate-outage was toggled on — see
 * docs/card_processor_mock_contract/api.md). Both cases trigger the same F4 alt-flow (ICT incident
 * + 503 to the frontend) so they are intentionally not distinguished by a separate exception type.
 */
public class CardProcessorUnavailableException extends RuntimeException {

    public CardProcessorUnavailableException(String message) {
        super(message);
    }

    public CardProcessorUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
