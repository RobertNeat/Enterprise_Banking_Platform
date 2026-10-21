package robert_neat.banking_backend.reporting;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Thin client for external_institutions_gateway_mock — mirrors CreditRiskClient/GovernanceClient's
 * structure/error mapping (see creditrisk/CreditRiskClient.java), but this mock returns
 * ProblemDetail (RFC 9457) bodies, not {"message": ...}, so extractMessage reads "detail" first
 * (same as GovernanceClient). Used by F6 (enforcement registration/transfer) and F7 (regulatory
 * report submission/status polling) — one client, one base-url property, since both flows hit the
 * same mock service (see docs/external_institutions_gateway_mock_contract/api.md).
 */
@Component
public class RegulatoryGatewayClient {

  private final RestClient restClient;

  public RegulatoryGatewayClient(
      RestClient.Builder restClientBuilder,
      @Value("${banking.regulatorygateway.base-url:http://localhost:10523}") String baseUrl) {
    this.restClient = restClientBuilder.baseUrl(baseUrl).build();
  }

  /** F6 — registers an incoming enforcement title with the gateway. protectedAmount may be null. */
  public EnforcementIncomingResponse registerEnforcement(
      String bailiff,
      String caseNumber,
      String customer,
      BigDecimal blockedAmount,
      BigDecimal protectedAmount) {
    Map<String, Object> body = new HashMap<>();
    body.put("bailiff", bailiff);
    body.put("caseNumber", caseNumber);
    body.put("customer", customer);
    body.put("blockedAmount", blockedAmount);
    body.put("protectedAmount", protectedAmount);
    return exchange(
        () ->
            restClient
                .post()
                .uri("/enforcement/incoming")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(EnforcementIncomingResponse.class));
  }

  /** F6 — transfers blocked funds to the bailiff via the gateway, settling the case there. */
  public EnforcementTransferResponse transferEnforcement(
      String gatewayEnforcementId, BigDecimal transferredAmount) {
    Map<String, Object> body = Map.of("transferredAmount", transferredAmount);
    return exchange(
        () ->
            restClient
                .post()
                .uri("/enforcement/{id}/transfer", gatewayEnforcementId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(EnforcementTransferResponse.class));
  }

  /** F7 — submits a regulatory report for KNF/NBP/BFG/FATCA-CRS review (async accept/reject). */
  public GatewaySubmissionResponse submit(
      String reportId, String recipient, String name, String period) {
    Map<String, Object> body = new HashMap<>();
    body.put("reportId", reportId);
    body.put("recipient", recipient);
    body.put("name", name);
    body.put("period", period);
    return exchange(
        () ->
            restClient
                .post()
                .uri("/regulatory-gateway/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(GatewaySubmissionResponse.class));
  }

  /** F7 — polls the gateway's current status for a previously submitted report. */
  public GatewaySubmissionResponse getSubmissionStatus(String submissionId) {
    return exchange(
        () ->
            restClient
                .get()
                .uri("/regulatory-gateway/submissions/{id}", submissionId)
                .retrieve()
                .body(GatewaySubmissionResponse.class));
  }

  private <T> T exchange(Supplier<T> call) {
    try {
      return call.get();
    } catch (HttpStatusCodeException ex) {
      throw new RegulatoryGatewayException(ex.getStatusCode(), extractMessage(ex));
    } catch (ResourceAccessException ex) {
      throw new RegulatoryGatewayUnavailableException(
          "external_institutions_gateway_mock unreachable", ex);
    }
  }

  /** ProblemDetail (RFC 9457) bodies carry the message under "detail", not "message". */
  private String extractMessage(HttpStatusCodeException ex) {
    try {
      JsonNode body = new ObjectMapper().readTree(ex.getResponseBodyAsByteArray());
      JsonNode detail = body.get("detail");
      JsonNode message = body.get("message");
      if (detail != null) {
        return detail.asText();
      }
      return message != null ? message.asText() : ex.getMessage();
    } catch (Exception parseFailure) {
      return ex.getMessage();
    }
  }
}
