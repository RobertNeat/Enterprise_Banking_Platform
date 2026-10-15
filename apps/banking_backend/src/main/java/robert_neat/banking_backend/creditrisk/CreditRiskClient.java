package robert_neat.banking_backend.creditrisk;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Thin client for credit_risk_mock — mirrors GovernanceClient's structure/error mapping
 * (see approvals/GovernanceClient.java). Scoring (F2) and ECL classification (F3, R4) both live
 * here since both hit the same mock service.
 */
@Component
public class CreditRiskClient {

    private final RestClient restClient;

    public CreditRiskClient(
            RestClient.Builder restClientBuilder,
            @Value("${banking.creditrisk.base-url:http://localhost:10522}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public CreditScoreResult score(String applicantId, BigDecimal requestedAmount, String segment) {
        Map<String, Object> body = Map.of(
                "applicantId", applicantId, "requestedAmount", requestedAmount, "segment", segment);
        return exchange(() -> restClient.post()
                .uri("/credit-bureau/score")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(CreditScoreResult.class));
    }

    /** F3 — classifies a collateral position into an IFRS 9 stage/provision bucket. */
    public EclClassificationResult classifyEcl(
            String positionId, int daysPastDue, String customerRating, BigDecimal exposureAmount) {
        Map<String, Object> body = Map.of(
                "positionId", positionId, "daysPastDue", daysPastDue,
                "customerRating", customerRating, "exposureAmount", exposureAmount);
        return exchange(() -> restClient.post()
                .uri("/ecl/classify")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(EclClassificationResult.class));
    }

    private <T> T exchange(Supplier<T> call) {
        try {
            return call.get();
        } catch (HttpStatusCodeException ex) {
            throw new CreditRiskException(ex.getStatusCode(), extractMessage(ex));
        } catch (ResourceAccessException ex) {
            throw new CreditRiskUnavailableException("credit_risk_mock unreachable", ex);
        }
    }

    private String extractMessage(HttpStatusCodeException ex) {
        try {
            JsonNode body = new ObjectMapper().readTree(ex.getResponseBodyAsByteArray());
            JsonNode message = body.get("message");
            return message != null ? message.asText() : ex.getMessage();
        } catch (Exception parseFailure) {
            return ex.getMessage();
        }
    }
}
