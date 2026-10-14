package robert_neat.credit_risk_mock.creditbureau;

import org.springframework.stereotype.Service;
import robert_neat.credit_risk_mock.common.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class CreditBureauService {

    private static final Set<String> SEGMENTS = Set.of("retail", "business");

    // applicantId -> chronological list of past inquiries (append-only history)
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<CreditBureauInquiry>> historyByApplicant =
            new ConcurrentHashMap<>();

    public CreditBureauInquiry score(ScoreRequest request) {
        validate(request);

        int scoreResult = deterministicScore(request.applicantId());
        BigDecimal dsti = deterministicDsti(request.applicantId(), request.requestedAmount());
        String inquiryId = UUID.randomUUID().toString();

        CreditBureauInquiry inquiry = new CreditBureauInquiry(
                request.applicantId(), scoreResult, dsti, inquiryId, Instant.now());

        historyByApplicant
                .computeIfAbsent(request.applicantId(), id -> new CopyOnWriteArrayList<>())
                .add(inquiry);

        return inquiry;
    }

    public List<CreditBureauInquiry> history(String applicantId) {
        return List.copyOf(historyByApplicant.getOrDefault(applicantId, new CopyOnWriteArrayList<>()));
    }

    private void validate(ScoreRequest request) {
        if (request.applicantId() == null || request.applicantId().isBlank()) {
            throw new ValidationException("applicantId is required");
        }
        if (request.requestedAmount() == null || request.requestedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("requestedAmount must be a positive amount");
        }
        if (request.segment() == null || !SEGMENTS.contains(request.segment())) {
            throw new ValidationException("segment must be one of: retail, business");
        }
    }

    /** Pseudo-random but deterministic 300-850 (FICO/BIK scale) score derived from applicantId hash. */
    private int deterministicScore(String applicantId) {
        return 300 + Math.floorMod(applicantId.hashCode(), 551);
    }

    /**
     * Pseudo-random DSTI in [0.0, 1.0], nudged upward as requestedAmount grows so the
     * mock stays plausible: larger loans correlate with a higher debt-service burden.
     */
    private BigDecimal deterministicDsti(String applicantId, BigDecimal requestedAmount) {
        int hash = Math.abs((applicantId + requestedAmount.toPlainString()).hashCode());
        double base = (hash % 1000) / 1000.0; // 0.0 - 0.999 pseudo-random base
        double amountFactor = Math.min(0.5, requestedAmount.doubleValue() / 1_000_000.0); // caps influence
        double dsti = Math.min(0.99, base * 0.6 + amountFactor);
        return BigDecimal.valueOf(dsti).setScale(2, RoundingMode.HALF_UP);
    }
}
