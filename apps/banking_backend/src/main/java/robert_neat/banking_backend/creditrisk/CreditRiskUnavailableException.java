package robert_neat.banking_backend.creditrisk;

/** Thrown when credit_risk_mock cannot be reached at all (connection failure, timeout). */
public class CreditRiskUnavailableException extends RuntimeException {

  public CreditRiskUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
