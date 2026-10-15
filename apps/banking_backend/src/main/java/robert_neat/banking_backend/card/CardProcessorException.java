package robert_neat.banking_backend.card;

import org.springframework.http.HttpStatusCode;

/** Wraps a 4xx error response passed through from card_processor_mock. */
public class CardProcessorException extends RuntimeException {

    private final HttpStatusCode status;

    public CardProcessorException(HttpStatusCode status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatusCode getStatus() {
        return status;
    }
}
