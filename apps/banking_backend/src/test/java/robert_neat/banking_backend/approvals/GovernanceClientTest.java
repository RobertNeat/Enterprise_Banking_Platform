package robert_neat.banking_backend.approvals;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * Exercises GovernanceClient's HTTP contract directly against MockRestServiceServer (see
 * auth/IdentityAccessClientTest for the same pattern) — catches query-param bugs that a
 * @MockitoBean on GovernanceClient in controller tests cannot, since those never build a real URI.
 */
class GovernanceClientTest {

    @Test
    void listAuditEventsSendsBothParamsWhenBothProvided() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GovernanceClient client = new GovernanceClient(builder, "http://governance-mock");

        server.expect(requestTo(
                        "http://governance-mock/audit-events?entityType=LOAN_APPLICATION&entityId=LN-80203"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(
                        "[{\"id\":\"evt-1\",\"actor\":\"PRC-10234\",\"action\":\"LOAN_STAGE_CHANGED\","
                                + "\"entityType\":\"LOAN_APPLICATION\",\"entityId\":\"LN-80203\","
                                + "\"timestamp\":\"2026-10-10T08:00:00Z\",\"details\":null}]",
                        MediaType.APPLICATION_JSON));

        List<GovernanceAuditEventResponse> result = client.listAuditEvents("LOAN_APPLICATION", "LN-80203");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("evt-1");
        assertThat(result.get(0).timestamp()).isEqualTo(Instant.parse("2026-10-10T08:00:00Z"));
        server.verify();
    }

    /**
     * Regression guard: a blank/null entityType or entityId must be OMITTED from the query string,
     * not sent as an empty string. governance_mock's AuditService.list() filters with
     * entityType.equalsIgnoreCase(...) / entityId.equals(...), so entityType= (empty) would match
     * nothing and silently return [] instead of "no filter" — the bug this test guards against.
     */
    @Test
    void listAuditEventsOmitsBlankParamsRatherThanSendingThemEmpty() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GovernanceClient client = new GovernanceClient(builder, "http://governance-mock");

        server.expect(requestTo("http://governance-mock/audit-events"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<GovernanceAuditEventResponse> result = client.listAuditEvents(null, null);

        assertThat(result).isEmpty();
        server.verify();
    }

    /** DSH-05 — mirrors POST /ict-incidents/{id}/resolve, the write-side of listIctIncidents(). */
    @Test
    void resolveIctIncidentPostsToResolveAndReturnsUpdatedIncident() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GovernanceClient client = new GovernanceClient(builder, "http://governance-mock");

        server.expect(requestTo("http://governance-mock/ict-incidents/inc-1/resolve"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"id\":\"inc-1\",\"source\":\"CARD_PROCESSOR\",\"description\":\"outage\","
                                + "\"severity\":\"HIGH\",\"status\":\"RESOLVED\","
                                + "\"createdAt\":\"2026-10-18T10:00:00Z\",\"resolvedAt\":\"2026-10-20T08:00:00Z\"}",
                        MediaType.APPLICATION_JSON));

        GovernanceIctIncidentResponse result = client.resolveIctIncident("inc-1");

        assertThat(result.id()).isEqualTo("inc-1");
        assertThat(result.status()).isEqualTo("RESOLVED");
        assertThat(result.resolvedAt()).isEqualTo(Instant.parse("2026-10-20T08:00:00Z"));
        server.verify();
    }

    /** governance_mock returns 409 for an already-resolved incident — passed through as GovernanceException. */
    @Test
    void resolveIctIncidentPropagatesConflictFromGovernanceMock() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GovernanceClient client = new GovernanceClient(builder, "http://governance-mock");

        server.expect(requestTo("http://governance-mock/ict-incidents/inc-1/resolve"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withStatus(org.springframework.http.HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"Incydent ICT juz rozwiazany: inc-1\"}"));

        assertThatThrownBy(() -> client.resolveIctIncident("inc-1"))
                .isInstanceOf(GovernanceException.class)
                .hasMessage("Incydent ICT juz rozwiazany: inc-1");
        server.verify();
    }
}
