package robert_neat.banking_backend.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import robert_neat.banking_backend.approvals.GovernanceClient;
import robert_neat.banking_backend.approvals.GovernanceIctIncidentResponse;
import robert_neat.banking_backend.approvals.GovernanceUnavailableException;

import java.time.Instant;
import java.util.List;

import static robert_neat.banking_backend.support.JwtTestSupport.testJwt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GovernanceClient is mocked (@MockitoBean) — no real HTTP call to governance_mock. Covers the
 * dashboard's ICT incidents (DORA) proxy read-side: GET /ict-incidents -> top N OPEN, newest first.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "BANKING_SEED_MOCK_DATA=false",
        "spring.datasource.url=jdbc:h2:mem:dashboard_ict_incidents;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
class DashboardIctIncidentsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GovernanceClient governanceClient;

    @Test
    void returnsOpenIncidentsNewestFirstExcludingResolved() throws Exception {
        Instant t1 = Instant.parse("2026-10-10T08:00:00Z");
        Instant t2 = Instant.parse("2026-10-15T09:00:00Z");
        Instant t3 = Instant.parse("2026-10-18T10:00:00Z");

        given(governanceClient.listIctIncidents()).willReturn(List.of(
                new GovernanceIctIncidentResponse("inc-1", "CARD_PROCESSOR", "outage 1", "HIGH", "OPEN", t1, null),
                new GovernanceIctIncidentResponse("inc-2", "EXTERNAL_INSTITUTIONS_GATEWAY", "outage 2", "MEDIUM",
                        "RESOLVED", t2, t2),
                new GovernanceIctIncidentResponse("inc-3", "CARD_PROCESSOR", "outage 3", "HIGH", "OPEN", t3, null)));

        mockMvc.perform(get("/dashboard/ict-incidents").with(testJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("inc-3"))
                .andExpect(jsonPath("$[1].id").value("inc-1"));

        verify(governanceClient).listIctIncidents();
    }

    @Test
    void limitsToTopFiveEvenWhenMoreOpenIncidentsExist() throws Exception {
        List<GovernanceIctIncidentResponse> sixOpenIncidents = java.util.stream.IntStream.range(0, 6)
                .mapToObj(i -> new GovernanceIctIncidentResponse(
                        "inc-" + i, "CARD_PROCESSOR", "outage " + i, "HIGH", "OPEN",
                        Instant.parse("2026-10-10T08:00:00Z").plusSeconds(i * 3600L), null))
                .toList();
        given(governanceClient.listIctIncidents()).willReturn(sixOpenIncidents);

        mockMvc.perform(get("/dashboard/ict-incidents").with(testJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].id").value("inc-5"));
    }

    @Test
    void returns503WhenGovernanceMockUnavailable() throws Exception {
        given(governanceClient.listIctIncidents())
                .willThrow(new GovernanceUnavailableException("down", new RuntimeException()));

        mockMvc.perform(get("/dashboard/ict-incidents").with(testJwt()))
                .andExpect(status().isServiceUnavailable());
    }
}
