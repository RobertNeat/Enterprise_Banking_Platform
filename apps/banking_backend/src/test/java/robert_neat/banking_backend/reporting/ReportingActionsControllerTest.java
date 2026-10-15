package robert_neat.banking_backend.reporting;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import robert_neat.banking_backend.approvals.GovernanceClient;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static robert_neat.banking_backend.support.JwtTestSupport.testJwt;

/**
 * RegulatoryGatewayClient and GovernanceClient are mocked (@MockitoBean). Seed: RPT-KNF-001
 * 'draft', RPT-NBP-014 'submitted' (with a PENDING report_submission row) — see
 * db/changelog/seed/010-reporting.sql.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "BANKING_SEED_MOCK_DATA=true",
        "spring.datasource.url=jdbc:h2:mem:reporting_actions;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
@Transactional
class ReportingActionsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegulatoryGatewayClient gatewayClient;

    @MockitoBean
    private GovernanceClient governanceClient;

    @Test
    void submitDraftReportMovesToSubmittedAndRecordsAudit() throws Exception {
        given(gatewayClient.submit(eq("RPT-KNF-001"), eq("KNF"), anyString(), anyString()))
                .willReturn(new GatewaySubmissionResponse("sub-uuid-1", "RPT-KNF-001", "KNF", "name",
                        "Q3 2026", "PENDING", List.of(), Instant.now(), null));

        mockMvc.perform(post("/reporting/reports/RPT-KNF-001/submit").with(testJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("submitted"));

        verify(governanceClient).recordAuditEvent(eq("PRC-00001"), eq("REPORT_SUBMITTED"),
                eq("REGULATORY_REPORT"), eq("RPT-KNF-001"), any());
    }

    @Test
    void submitNonDraftReportIsConflict() throws Exception {
        // RPT-NBP-014 is seeded 'submitted', not 'draft'.
        mockMvc.perform(post("/reporting/reports/RPT-NBP-014/submit").with(testJwt()))
                .andExpect(status().isConflict());

        verify(gatewayClient, never()).submit(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void submitReturns503AndRecordsIctIncidentWhenGatewayUnavailable() throws Exception {
        given(gatewayClient.submit(anyString(), anyString(), anyString(), anyString()))
                .willThrow(new RegulatoryGatewayUnavailableException("down", new RuntimeException()));

        mockMvc.perform(post("/reporting/reports/RPT-KNF-001/submit").with(testJwt()))
                .andExpect(status().isServiceUnavailable());

        verify(governanceClient).createIctIncident(eq("EXTERNAL_INSTITUTIONS_GATEWAY"), anyString(), eq("HIGH"));
    }

    @Test
    void refreshSubmissionStatusAcceptedMovesReportToAccepted() throws Exception {
        given(gatewayClient.getSubmissionStatus("7f3c2a91-4b6e-4d2a-9c1f-2e8b5d6a0f14"))
                .willReturn(new GatewaySubmissionResponse("7f3c2a91-4b6e-4d2a-9c1f-2e8b5d6a0f14", "RPT-NBP-014",
                        "NBP", "name", "Wrzesień 2026", "ACCEPTED", List.of(), Instant.now(), Instant.now()));

        mockMvc.perform(post("/reporting/reports/RPT-NBP-014/refresh-submission-status").with(testJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.report.status").value("accepted"))
                .andExpect(jsonPath("$.submission.gatewayStatus").value("ACCEPTED"));
    }

    @Test
    void refreshSubmissionStatusRejectedMovesReportBackToDraft() throws Exception {
        given(gatewayClient.getSubmissionStatus("7f3c2a91-4b6e-4d2a-9c1f-2e8b5d6a0f14"))
                .willReturn(new GatewaySubmissionResponse("7f3c2a91-4b6e-4d2a-9c1f-2e8b5d6a0f14", "RPT-NBP-014",
                        "NBP", "name", "Wrzesień 2026", "REJECTED",
                        List.of("Niepoprawny format pliku"), Instant.now(), Instant.now()));

        mockMvc.perform(post("/reporting/reports/RPT-NBP-014/refresh-submission-status").with(testJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.report.status").value("draft"))
                .andExpect(jsonPath("$.submission.gatewayStatus").value("REJECTED"))
                .andExpect(jsonPath("$.submission.validationErrors").value("Niepoprawny format pliku"));
    }

    @Test
    void refreshSubmissionStatusOnDraftReportIsConflict() throws Exception {
        mockMvc.perform(post("/reporting/reports/RPT-KNF-001/refresh-submission-status").with(testJwt()))
                .andExpect(status().isConflict());
    }

    @Test
    void submitRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/reporting/reports/RPT-KNF-001/submit"))
                .andExpect(status().isUnauthorized());
    }
}
