package robert_neat.banking_backend.common;

/** Thrown when a requested stage/status transition is not valid from the entity's current state. */
public class InvalidStageTransitionException extends RuntimeException {

  public InvalidStageTransitionException(String message) {
    super(message);
  }
}
