package robert_neat.banking_backend.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import robert_neat.banking_backend.approvals.GovernanceClient;
import robert_neat.banking_backend.approvals.GovernanceException;
import robert_neat.banking_backend.approvals.GovernanceIctIncidentResponse;
import robert_neat.banking_backend.approvals.GovernanceUnavailableException;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static robert_neat.banking_backend.support.JwtTestSupport.testJwt;

/**
 * DSH-05 — POST /dashboard/ict-incidents/{id}/resolve proxies governance_mock's own endpoint.
 * GovernanceClient is mocked (@MockitoBean) — no real HTTP calls, same convention as
 * cases/CaseActionsControllerTest. GovernanceClient's own HTTP contract (query params, URI,
 * response mapping) is covered separately by approvals/GovernanceClientTest with a real
 * MockRestServiceServer.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "BANKING_SEED_MOCK_DATA=true",
        "spring.datasource.url=jdbc:h2:mem:dashboard_controller;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
@Transactional
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GovernanceClient governanceClient;

    @Test
    void resolveIctIncidentProxiesToGovernanceMockAndReturnsUpdatedIncident() throws Exception {
        given(governanceClient.resolveIctIncident("inc-1")).willReturn(new GovernanceIctIncidentResponse(
                "inc-1", "CARD_PROCESSOR", "card_processor_mock unavailable", "HIGH", "RESOLVED",
                Instant.parse("2026-10-18T10:00:00Z"), Instant.parse("2026-10-20T08:00:00Z")));

        mockMvc.perform(post("/dashboard/ict-incidents/inc-1/resolve").with(testJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("inc-1"))
                .andExpect(jsonPath("$.source").value("CARD_PROCESSOR"))
                .andExpect(jsonPath("$.severity").value("HIGH"));

        verify(governanceClient).resolveIctIncident("inc-1");
    }

    @Test
    void resolveIctIncidentPropagatesConflictWhenAlreadyResolved() throws Exception {
        given(governanceClient.resolveIctIncident(eq("inc-1")))
                .willThrow(new GovernanceException(CONFLICT, "Incydent ICT juz rozwiazany: inc-1"));

        mockMvc.perform(post("/dashboard/ict-incidents/inc-1/resolve").with(testJwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Incydent ICT juz rozwiazany: inc-1"));
    }

    @Test
    void resolveIctIncidentReturns503WhenGovernanceMockUnavailable() throws Exception {
        given(governanceClient.resolveIctIncident(eq("inc-1")))
                .willThrow(new GovernanceUnavailableException("down", new RuntimeException()));

        mockMvc.perform(post("/dashboard/ict-incidents/inc-1/resolve").with(testJwt()))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void resolveIctIncidentRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/dashboard/ict-incidents/inc-1/resolve"))
                .andExpect(status().isUnauthorized());
    }
}
