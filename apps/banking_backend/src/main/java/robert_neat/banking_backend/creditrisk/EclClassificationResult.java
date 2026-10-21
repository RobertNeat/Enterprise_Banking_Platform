package robert_neat.banking_backend.creditrisk;

import java.math.BigDecimal;
import java.time.Instant;

/** Mirrors credit_risk_mock's POST /ecl/classify response shape (see CreditScoreResult). */
public record EclClassificationResult(
    String positionId, String stage, BigDecimal provision, Instant classifiedAt) {}
