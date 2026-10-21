package robert_neat.banking_backend.approvals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static robert_neat.banking_backend.support.JwtTestSupport.testJwt;

/**
 * GovernanceClient is mocked (@MockitoBean) — no real HTTP call to governance_mock. Covers the
 * "Historia" tab/dialog proxy: GET /audit-events?entityType&entityId.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "BANKING_SEED_MOCK_DATA=false",
        "spring.datasource.url=jdbc:h2:mem:audit_events_controller;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
class AuditEventsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GovernanceClient governanceClient;

    @Test
    void listPassesEntityTypeAndEntityIdThroughToGovernanceClient() throws Exception {
        // 2026-10-10T08:00:00Z is 2026-10-10 10:00 in Europe/Warsaw (CEST, +02:00 in October).
        Instant t1 = Instant.parse("2026-10-10T08:00:00Z");
        given(governanceClient.listAuditEvents("LOAN_APPLICATION", "LN-80203")).willReturn(List.of(
                new GovernanceAuditEventResponse(
                        "evt-1", "PRC-10234", "LOAN_STAGE_CHANGED", "LOAN_APPLICATION", "LN-80203", t1,
                        "application -> decision")));

        mockMvc.perform(get("/audit-events")
                        .param("entityType", "LOAN_APPLICATION")
                        .param("entityId", "LN-80203")
                        .with(testJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("evt-1"))
                .andExpect(jsonPath("$[0].action").value("LOAN_STAGE_CHANGED"))
                .andExpect(jsonPath("$[0].timestamp").value("2026-10-10 10:00"))
                .andExpect(jsonPath("$[0].details").value("application -> decision"));

        verify(governanceClient).listAuditEvents("LOAN_APPLICATION", "LN-80203");
    }

    @Test
    void listWithoutQueryParamsPassesNullsThrough() throws Exception {
        given(governanceClient.listAuditEvents(null, null)).willReturn(List.of());

        mockMvc.perform(get("/audit-events").with(testJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(governanceClient).listAuditEvents(null, null);
    }

    @Test
    void returns503WhenGovernanceMockUnavailable() throws Exception {
        given(governanceClient.listAuditEvents("CUSTOMER_CASE", "CASE-1"))
                .willThrow(new GovernanceUnavailableException("down", new RuntimeException()));

        mockMvc.perform(get("/audit-events")
                        .param("entityType", "CUSTOMER_CASE")
                        .param("entityId", "CASE-1")
                        .with(testJwt()))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/audit-events")
                        .param("entityType", "CUSTOMER_CASE")
                        .param("entityId", "CASE-1"))
                .andExpect(status().isUnauthorized());
    }
}
