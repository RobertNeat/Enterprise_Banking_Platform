package robert_neat.banking_backend.card;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Thin client for card_processor_mock — mirrors CreditRiskClient/GovernanceClient's
 * structure/error mapping (see creditrisk/CreditRiskClient.java). F4 (card block/unblock/
 * authorization/chargeback).
 */
@Component
public class CardProcessorClient {

    private final RestClient restClient;

    public CardProcessorClient(
            RestClient.Builder restClientBuilder,
            @Value("${banking.cardprocessor.base-url:http://localhost:10521}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public CardProcessorAuthorizationResponse authorize(
            String cardId, String merchant, BigDecimal amount, String mcc, String processor) {
        Map<String, Object> body = new HashMap<>();
        body.put("cardId", cardId);
        body.put("merchant", merchant);
        body.put("amount", amount);
        body.put("mcc", mcc);
        if (processor != null && !processor.isBlank()) {
            body.put("processor", processor);
        }
        return exchange(() -> restClient.post()
                .uri("/authorizations")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(CardProcessorAuthorizationResponse.class));
    }

    public CardBlockStatusResponse blockCard(String cardId) {
        return exchange(() -> restClient.post()
                .uri("/cards/{cardId}/block", cardId)
                .retrieve()
                .body(CardBlockStatusResponse.class));
    }

    public CardBlockStatusResponse unblockCard(String cardId) {
        return exchange(() -> restClient.post()
                .uri("/cards/{cardId}/unblock", cardId)
                .retrieve()
                .body(CardBlockStatusResponse.class));
    }

    public CardBlockStatusResponse getBlockStatus(String cardId) {
        return exchange(() -> restClient.get()
                .uri("/cards/{cardId}/block-status", cardId)
                .retrieve()
                .body(CardBlockStatusResponse.class));
    }

    public CardProcessorChargebackResponse fileChargeback(
            String authorizationId, String reasonCode, String description) {
        Map<String, String> body = Map.of(
                "authorizationId", authorizationId, "reasonCode", reasonCode, "description", description);
        return exchange(() -> restClient.post()
                .uri("/chargebacks")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(CardProcessorChargebackResponse.class));
    }

    public CardProcessorChargebackResponse advanceChargeback(String id, String status) {
        Map<String, String> body = Map.of("status", status);
        return exchange(() -> restClient.post()
                .uri("/chargebacks/{id}/advance", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(CardProcessorChargebackResponse.class));
    }

    private <T> T exchange(Supplier<T> call) {
        try {
            return call.get();
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                // Simulated outage (not a connection failure) — see
                // docs/card_processor_mock_contract/api.md, POST /processor/simulate-outage.
                throw new CardProcessorUnavailableException(
                        "card_processor_mock outage simulated: " + extractMessage(ex), ex);
            }
            throw new CardProcessorException(ex.getStatusCode(), extractMessage(ex));
        } catch (ResourceAccessException ex) {
            throw new CardProcessorUnavailableException("card_processor_mock unreachable", ex);
        }
    }

    /** Nest validation errors may return "message" as a string OR an array of strings. */
    private String extractMessage(HttpStatusCodeException ex) {
        try {
            JsonNode body = new ObjectMapper().readTree(ex.getResponseBodyAsByteArray());
            JsonNode message = body.get("message");
            if (message == null) {
                return ex.getMessage();
            }
            if (message.isArray()) {
                StringBuilder joined = new StringBuilder();
                message.forEach(item -> {
                    if (!joined.isEmpty()) {
                        joined.append("; ");
                    }
                    joined.append(item.asText());
                });
                return joined.toString();
            }
            return message.asText();
        } catch (Exception parseFailure) {
            return ex.getMessage();
        }
    }
}
