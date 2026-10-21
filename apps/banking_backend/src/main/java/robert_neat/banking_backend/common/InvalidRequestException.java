package robert_neat.banking_backend.common;

/**
 * Thrown when a request body fails a validation rule (e.g. a field value outside the set accepted
 * by a database CHECK constraint); mapped to HTTP 400. Must be thrown before any side effect
 * (outbound mock call, DB write) so a bad request never consumes an external resource.
 */
public class InvalidRequestException extends RuntimeException {

  public InvalidRequestException(String message) {
    super(message);
  }
}
