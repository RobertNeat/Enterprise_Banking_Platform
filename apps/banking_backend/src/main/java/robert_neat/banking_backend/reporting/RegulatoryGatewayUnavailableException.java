package robert_neat.banking_backend.reporting;

/**
 * Thrown when external_institutions_gateway_mock cannot be reached at all (connection failure,
 * timeout).
 */
public class RegulatoryGatewayUnavailableException extends RuntimeException {

  public RegulatoryGatewayUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
