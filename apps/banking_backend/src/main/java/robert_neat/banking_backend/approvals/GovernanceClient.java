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
 * Thin client for governance_mock. Approval queue (maker-checker) used since R1.5; recordAuditEvent
 * and slaCheck added in R2 for F2 (loan stage audit trail) and F8 (case SLA escalation).
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

    /** Mirrors POST /audit-events. Returns the generated audit event id (governance_mock assigns it). */
    public String recordAuditEvent(String actor, String action, String entityType, String entityId, String details) {
        Map<String, String> body = Map.of(
                "actor", actor, "action", action, "entityType", entityType, "entityId", entityId,
                "details", details == null ? "" : details);
        AuditEventResponse response = exchange(() -> restClient.post()
                .uri("/audit-events")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(AuditEventResponse.class));
        return response.id();
    }

    /** Mirrors POST /cases/{caseId}/sla-check — F8 SLA escalation rule evaluated by governance_mock. */
    public SlaCheckResult slaCheck(String caseId, long slaRemainingHours) {
        Map<String, Long> body = Map.of("slaRemainingHours", slaRemainingHours);
        return exchange(() -> restClient.post()
                .uri("/cases/{caseId}/sla-check", caseId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(SlaCheckResult.class));
    }

    /**
     * Mirrors POST /ict-incidents (DORA ICT incident registry) — used by F4's "processor
     * unavailable" alt-flow (see card/CardActionsService.java) to record a HIGH-severity incident
     * before propagating a 503 to the frontend. Reusable by any future round hitting the same
     * "mock unavailable -> ICT incident" pattern (see docs/services/banking_backend.md).
     */
    public void createIctIncident(String source, String description, String severity) {
        Map<String, String> body = Map.of("source", source, "description", description, "severity", severity);
        exchange(() -> {
            restClient.post()
                    .uri("/ict-incidents")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return null;
        });
    }

    /** Mirrors GET /ict-incidents — used by the dashboard's ICT incidents card (DORA, F4 read-side). */
    public List<GovernanceIctIncidentResponse> listIctIncidents() {
        GovernanceIctIncidentResponse[] responses = exchange(() -> restClient.get()
                .uri("/ict-incidents")
                .retrieve()
                .body(GovernanceIctIncidentResponse[].class));
        return responses == null ? List.of() : List.of(responses);
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
