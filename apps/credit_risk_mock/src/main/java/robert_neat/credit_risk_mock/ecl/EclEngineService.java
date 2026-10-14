package robert_neat.credit_risk_mock.ecl;

import org.springframework.stereotype.Service;
import robert_neat.credit_risk_mock.common.NotFoundException;
import robert_neat.credit_risk_mock.common.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EclEngineService {

    private static final BigDecimal STAGE1_RATE = new BigDecimal("0.01");
    private static final BigDecimal STAGE2_RATE = new BigDecimal("0.05");
    private static final BigDecimal STAGE3_RATE = new BigDecimal("0.25");

    // positionId -> most recent classification
    private final ConcurrentHashMap<String, EclClassification> latestByPosition = new ConcurrentHashMap<>();

    public EclClassification classify(ClassifyRequest request) {
        validate(request);

        String stage = resolveStage(request.daysPastDue());
        BigDecimal provision = request.exposureAmount()
                .multiply(rateForStage(stage))
                .setScale(2, RoundingMode.HALF_UP);

        EclClassification classification = new EclClassification(
                request.positionId(), stage, provision, Instant.now());

        latestByPosition.put(request.positionId(), classification);
        return classification;
    }

    public EclClassification latest(String positionId) {
        EclClassification classification = latestByPosition.get(positionId);
        if (classification == null) {
            throw new NotFoundException("No ECL classification found for positionId: " + positionId);
        }
        return classification;
    }

    private void validate(ClassifyRequest request) {
        if (request.positionId() == null || request.positionId().isBlank()) {
            throw new ValidationException("positionId is required");
        }
        if (request.daysPastDue() < 0) {
            throw new ValidationException("daysPastDue must not be negative");
        }
        if (request.customerRating() == null || request.customerRating().isBlank()) {
            throw new ValidationException("customerRating is required");
        }
        if (request.exposureAmount() == null || request.exposureAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("exposureAmount must be a positive amount");
        }
    }

    private String resolveStage(int daysPastDue) {
        if (daysPastDue <= 30) {
            return "stage1";
        }
        if (daysPastDue <= 90) {
            return "stage2";
        }
        return "stage3";
    }

    private BigDecimal rateForStage(String stage) {
        return switch (stage) {
            case "stage1" -> STAGE1_RATE;
            case "stage2" -> STAGE2_RATE;
            default -> STAGE3_RATE;
        };
    }
}
