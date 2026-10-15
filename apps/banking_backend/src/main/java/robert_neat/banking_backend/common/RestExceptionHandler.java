package robert_neat.banking_backend.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import robert_neat.banking_backend.auth.IdentityAccessException;
import robert_neat.banking_backend.auth.IdentityAccessUnavailableException;
import robert_neat.banking_backend.auth.UnmappedRoleException;
import robert_neat.banking_backend.approvals.GovernanceException;
import robert_neat.banking_backend.approvals.GovernanceUnavailableException;
import robert_neat.banking_backend.approvals.InsufficientSeniorityException;
import robert_neat.banking_backend.creditrisk.CreditRiskException;
import robert_neat.banking_backend.creditrisk.CreditRiskUnavailableException;
import robert_neat.banking_backend.card.CardProcessorException;
import robert_neat.banking_backend.card.CardProcessorUnavailableException;
import robert_neat.banking_backend.ebanking.OtpNotVerifiedException;
import robert_neat.banking_backend.common.InvalidStageTransitionException;

@RestControllerAdvice(annotations = RestController.class)
public class RestExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(ex.getMessage()));
    }

    /** Passes identity_access_mock's own status/message through unchanged (401/403/400/409). */
    @ExceptionHandler(IdentityAccessException.class)
    public ResponseEntity<ApiError> handleIdentityAccess(IdentityAccessException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler(IdentityAccessUnavailableException.class)
    public ResponseEntity<ApiError> handleIdentityAccessUnavailable(IdentityAccessUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiError("identity_access_mock unavailable"));
    }

    /** identity_access_mock returned a role banking_backend doesn't recognize — an upstream contract fault. */
    @ExceptionHandler(UnmappedRoleException.class)
    public ResponseEntity<ApiError> handleUnmappedRole(UnmappedRoleException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ApiError(ex.getMessage()));
    }

    /** Maker-checker gate failed (insufficient seniority or self-approval) — rejected before governance_mock. */
    @ExceptionHandler(InsufficientSeniorityException.class)
    public ResponseEntity<ApiError> handleInsufficientSeniority(InsufficientSeniorityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(ex.getMessage()));
    }

    /** Passes governance_mock's own status/message through unchanged (400/404/409). */
    @ExceptionHandler(GovernanceException.class)
    public ResponseEntity<ApiError> handleGovernance(GovernanceException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler(GovernanceUnavailableException.class)
    public ResponseEntity<ApiError> handleGovernanceUnavailable(GovernanceUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiError("governance_mock unavailable"));
    }

    /** Passes credit_risk_mock's own status/message through unchanged (400). */
    @ExceptionHandler(CreditRiskException.class)
    public ResponseEntity<ApiError> handleCreditRisk(CreditRiskException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler(CreditRiskUnavailableException.class)
    public ResponseEntity<ApiError> handleCreditRiskUnavailable(CreditRiskUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiError("credit_risk_mock unavailable"));
    }

    /** Stage/status precondition failed (e.g. loan not in 'application', case already escalated). */
    @ExceptionHandler(InvalidStageTransitionException.class)
    public ResponseEntity<ApiError> handleInvalidStageTransition(InvalidStageTransitionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(ex.getMessage()));
    }

    /** Passes card_processor_mock's own status/message through unchanged (400/404/409). */
    @ExceptionHandler(CardProcessorException.class)
    public ResponseEntity<ApiError> handleCardProcessor(CardProcessorException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new ApiError(ex.getMessage()));
    }

    /** Covers both a real connection failure and the processor's own simulated outage (503). */
    @ExceptionHandler(CardProcessorUnavailableException.class)
    public ResponseEntity<ApiError> handleCardProcessorUnavailable(CardProcessorUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiError("card_processor_mock unavailable"));
    }

    /** OTP code was wrong/expired/already used (identity_access_mock returned verified:false). */
    @ExceptionHandler(OtpNotVerifiedException.class)
    public ResponseEntity<ApiError> handleOtpNotVerified(OtpNotVerifiedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(ex.getMessage()));
    }
}
