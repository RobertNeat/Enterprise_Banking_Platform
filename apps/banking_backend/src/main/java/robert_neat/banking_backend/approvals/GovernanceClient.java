package robert_neat.banking_backend.approvals;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Thin client for governance_mock. Built to be reused beyond approvals (R2+ needs audit-events
 * and, later, ICT incidents / SLA checks for F2/F8) — only the approval-queue methods actually
 * used by R1.5 are implemented; recordAuditEvent's signature is defined now so R2 extends this
 * client instead of writing a second one, but its body is intentionally not wired into any
 * endpoint yet (no caller in this round).
 */
@Component
public class GovernanceClient {

    private final RestClient restClient;

    public GovernanceClient(
            RestClient.Builder restClientBuilder,
            @Value("${banking.governance.base-url:http://localhost:10524}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public ApprovalRecord createApproval(String type, String requestedBy, String payload) {
        Map<String, String> body = Map.of("type", type, "requestedBy", requestedBy,
                "payload", payload == null ? "" : payload);
        GovernanceApprovalResponse response = exchange(() -> restClient.post()
                .uri("/approvals")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(GovernanceApprovalResponse.class));
        return response.toApprovalRecord();
    }

    public List<ApprovalRecord> listApprovals(String status) {
        GovernanceApprovalResponse[] responses = exchange(() -> restClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/approvals");
                    if (status != null && !status.isBlank()) {
                        builder.queryParam("status", status);
                    }
                    return builder.build();
                })
                .retrieve()
                .body(GovernanceApprovalResponse[].class));
        return responses == null ? List.of()
                : List.of(responses).stream().map(GovernanceApprovalResponse::toApprovalRecord).toList();
    }

    public ApprovalRecord getApproval(String id) {
        GovernanceApprovalResponse response = exchange(() -> restClient.get()
                .uri("/approvals/{id}", id)
                .retrieve()
                .body(GovernanceApprovalResponse.class));
        return response.toApprovalRecord();
    }

    public ApprovalRecord decideApproval(String id, String decision, String decidedBy) {
        Map<String, String> body = Map.of("decision", decision, "decidedBy", decidedBy);
        GovernanceApprovalResponse response = exchange(() -> restClient.post()
                .uri("/approvals/{id}/decide", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(GovernanceApprovalResponse.class));
        return response.toApprovalRecord();
    }

    /**
     * Not called anywhere yet in R1.5 — defined so R2's audit trail work (F2/F8) extends this
     * client rather than duplicating the HTTP plumbing. Mirrors POST /audit-events.
     */
    public void recordAuditEvent(String actor, String action, String entityType, String entityId, String details) {
        Map<String, String> body = Map.of(
                "actor", actor, "action", action, "entityType", entityType, "entityId", entityId,
                "details", details == null ? "" : details);
        exchange(() -> {
            restClient.post()
                    .uri("/audit-events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return null;
        });
    }

    private <T> T exchange(Supplier<T> call) {
        try {
            return call.get();
        } catch (HttpStatusCodeException ex) {
            throw new GovernanceException(ex.getStatusCode(), extractMessage(ex));
        } catch (ResourceAccessException ex) {
            throw new GovernanceUnavailableException("governance_mock unreachable", ex);
        }
    }

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
