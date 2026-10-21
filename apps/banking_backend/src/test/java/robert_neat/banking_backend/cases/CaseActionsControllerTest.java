package robert_neat.banking_backend.cases;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import robert_neat.banking_backend.approvals.GovernanceClient;
import robert_neat.banking_backend.approvals.GovernanceUnavailableException;
import robert_neat.banking_backend.approvals.SlaCheckResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static robert_neat.banking_backend.support.JwtTestSupport.testJwt;

/**
 * GovernanceClient is mocked (@MockitoBean) — no real HTTP calls. testJwt() defaults to subject
 * PRC-00001 (Administrator systemu, seeded employee row).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "BANKING_SEED_MOCK_DATA=true",
        "spring.datasource.url=jdbc:h2:mem:case_actions;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
@Transactional
class CaseActionsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CaseActionsService caseActionsService;

    @Autowired
    private CustomerCaseRepository customerCases;

    @MockitoBean
    private GovernanceClient governanceClient;

    @Test
    void createComputesStatutoryDeadlineAndSlaInsteadOfAcceptingThemFromCaller() throws Exception {
        mockMvc.perform(post("/cases").with(testJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subject":"Reklamacja opłaty","customerId":"CUST-001","ownerId":"PRC-60001",
                                 "priority":"high","caseType":"complaint"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("new"))
                .andExpect(jsonPath("$.priority").value("high"))
                .andExpect(jsonPath("$.statutoryDeadline").isNotEmpty())
                .andExpect(jsonPath("$.slaRemainingHours").value(24));
    }

    @Test
    void createDefaultsOwnerToJwtCallerWhenOwnerIdOmitted() throws Exception {
        mockMvc.perform(post("/cases").with(testJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subject":"Zapytanie","customerId":"CUST-002",
                                 "priority":"low","caseType":"inquiry"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value("K. Malinowski"));
    }

    @Test
    void escalateWithSlaExpiredDelegatesAutoEscalationToGovernanceMockWithoutExtraAuditCall() throws Exception {
        given(governanceClient.slaCheck(eq("CS-10225"), anyLong()))
                .willReturn(new SlaCheckResult("CS-10225", true, "audit-auto-1"));

        mockMvc.perform(post("/cases/CS-10225/escalate").with(testJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.escalated").value(true))
                .andExpect(jsonPath("$.customerCase.status").value("escalated"));

        verify(governanceClient, never()).recordAuditEvent(anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void escalateWithSlaNotExpiredRecordsManualAuditEvent() throws Exception {
        given(governanceClient.slaCheck(eq("CS-10225"), anyLong()))
                .willReturn(new SlaCheckResult("CS-10225", false, null));
        given(governanceClient.recordAuditEvent(eq("PRC-00001"), eq("CASE_ESCALATED_MANUALLY"),
                eq("CUSTOMER_CASE"), eq("CS-10225"), anyString()))
                .willReturn("audit-manual-1");

        mockMvc.perform(post("/cases/CS-10225/escalate").with(testJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.escalated").value(false))
                .andExpect(jsonPath("$.customerCase.status").value("escalated"));

        verify(governanceClient).recordAuditEvent(eq("PRC-00001"), eq("CASE_ESCALATED_MANUALLY"),
                eq("CUSTOMER_CASE"), eq("CS-10225"), anyString());
    }

    @Test
    void escalateOnAlreadyEscalatedCaseIsConflict() throws Exception {
        // CS-10231 is seeded with status 'escalated'.
        mockMvc.perform(post("/cases/CS-10231/escalate").with(testJwt()))
                .andExpect(status().isConflict());

        verify(governanceClient, never()).slaCheck(anyString(), anyLong());
    }

    @Test
    void escalateOnResolvedCaseIsConflict() throws Exception {
        // CS-10219 is seeded with status 'resolved'.
        mockMvc.perform(post("/cases/CS-10219/escalate").with(testJwt()))
                .andExpect(status().isConflict());
    }

    @Test
    void escalateReturns503WhenGovernanceMockUnavailable() throws Exception {
        given(governanceClient.slaCheck(anyString(), anyLong()))
                .willThrow(new GovernanceUnavailableException("down", new RuntimeException()));

        mockMvc.perform(post("/cases/CS-10225/escalate").with(testJwt()))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void autoEscalateIfSlaExpiredEscalatesWithoutAnExtraAuditCall() {
        // CS-10225 is seeded with status 'new'. F8 scheduled sweep path (CaseSlaEscalationScheduler)
        // runs outside any request/JWT context, so this calls the service method directly.
        given(governanceClient.slaCheck(eq("CS-10225"), anyLong()))
                .willReturn(new SlaCheckResult("CS-10225", true, "audit-auto-sweep-1"));

        caseActionsService.autoEscalateIfSlaExpired("CS-10225");

        CustomerCase updated = customerCases.findById("CS-10225").orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("escalated");
        assertThat(updated.getEscalationSource()).isEqualTo("AUTO_SLA");
        assertThat(updated.getGovernanceAuditEventId()).isEqualTo("audit-auto-sweep-1");
        verify(governanceClient, never()).recordAuditEvent(anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void autoEscalateIfSlaExpiredIsNoOpWhenGovernanceMockSaysNotEscalated() {
        // Defensive race case: the scheduler's status filter is stale, or slaDueAt is right at the
        // boundary — governance_mock is still the source of truth, so nothing is written.
        given(governanceClient.slaCheck(eq("CS-10225"), anyLong()))
                .willReturn(new SlaCheckResult("CS-10225", false, null));

        caseActionsService.autoEscalateIfSlaExpired("CS-10225");

        CustomerCase updated = customerCases.findById("CS-10225").orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("new");
        assertThat(updated.getEscalationSource()).isNull();
        verify(governanceClient, never()).recordAuditEvent(anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void autoEscalateIfSlaExpiredOnAlreadyEscalatedCaseIsNoOpAndSkipsGovernanceCall() {
        // CS-10231 is seeded with status 'escalated' — the status filter itself is the dedup guard,
        // but the service method double-checks defensively (status filter in the repo query could
        // race a concurrent manual escalate between query and this call).
        caseActionsService.autoEscalateIfSlaExpired("CS-10231");

        verify(governanceClient, never()).slaCheck(anyString(), anyLong());
    }

    @Test
    void createWithInvalidPriorityIsBadRequestAndNeverTouchesGovernanceMock() throws Exception {
        mockMvc.perform(post("/cases").with(testJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subject":"Reklamacja opłaty","customerId":"CUST-001",
                                 "priority":"urgent","caseType":"complaint"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "priority must be one of: low, medium, high"));

        verifyNoInteractions(governanceClient);
    }

    @Test
    void createWithNullPriorityIsBadRequest() throws Exception {
        mockMvc.perform(post("/cases").with(testJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subject":"Reklamacja opłaty","customerId":"CUST-001",
                                 "caseType":"complaint"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "priority must be one of: low, medium, high"));

        verifyNoInteractions(governanceClient);
    }

    @Test
    void createWithBlankCaseTypeIsBadRequest() throws Exception {
        mockMvc.perform(post("/cases").with(testJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subject":"Reklamacja opłaty","customerId":"CUST-001",
                                 "priority":"high","caseType":""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("caseType must not be blank"));

        verifyNoInteractions(governanceClient);
    }

    @Test
    void createRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void startOnNewCaseMovesToInProgressAndAudits() throws Exception {
        // CS-10225 is seeded with status 'new'.
        given(governanceClient.recordAuditEvent(eq("PRC-00001"), eq("CASE_STATUS_CHANGED"),
                eq("CUSTOMER_CASE"), eq("CS-10225"), anyString()))
                .willReturn("audit-start-1");

        mockMvc.perform(post("/cases/CS-10225/start").with(testJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("inProgress"));

        verify(governanceClient).recordAuditEvent(eq("PRC-00001"), eq("CASE_STATUS_CHANGED"),
                eq("CUSTOMER_CASE"), eq("CS-10225"), eq("new -> inProgress"));
    }

    @Test
    void startOnInProgressCaseIsConflict() throws Exception {
        // CS-10229 is seeded with status 'inProgress'.
        mockMvc.perform(post("/cases/CS-10229/start").with(testJwt()))
                .andExpect(status().isConflict());

        verifyNoInteractions(governanceClient);
    }

    @Test
    void startOnEscalatedCaseIsConflict() throws Exception {
        // CS-10231 is seeded with status 'escalated'.
        mockMvc.perform(post("/cases/CS-10231/start").with(testJwt()))
                .andExpect(status().isConflict());

        verifyNoInteractions(governanceClient);
    }

    @Test
    void startOnResolvedCaseIsConflict() throws Exception {
        // CS-10219 is seeded with status 'resolved'.
        mockMvc.perform(post("/cases/CS-10219/start").with(testJwt()))
                .andExpect(status().isConflict());

        verifyNoInteractions(governanceClient);
    }

    @Test
    void startRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/cases/CS-10225/start"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resolveOnNewCaseMovesToResolvedAndAudits() throws Exception {
        // CS-10225 is seeded with status 'new'.
        given(governanceClient.recordAuditEvent(eq("PRC-00001"), eq("CASE_STATUS_CHANGED"),
                eq("CUSTOMER_CASE"), eq("CS-10225"), anyString()))
                .willReturn("audit-resolve-1");

        mockMvc.perform(post("/cases/CS-10225/resolve").with(testJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("resolved"));

        verify(governanceClient).recordAuditEvent(eq("PRC-00001"), eq("CASE_STATUS_CHANGED"),
                eq("CUSTOMER_CASE"), eq("CS-10225"), eq("new -> resolved"));
    }

    @Test
    void resolveOnInProgressCaseMovesToResolved() throws Exception {
        // CS-10229 is seeded with status 'inProgress'.
        mockMvc.perform(post("/cases/CS-10229/resolve").with(testJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("resolved"));
    }

    @Test
    void resolveOnEscalatedCaseMovesToResolved() throws Exception {
        // CS-10231 is seeded with status 'escalated' — this is the key path per the CAS plan item:
        // a case only ever leaves 'escalated' via resolve.
        mockMvc.perform(post("/cases/CS-10231/resolve").with(testJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("resolved"));
    }

    @Test
    void resolveOnAlreadyResolvedCaseIsConflict() throws Exception {
        // CS-10219 is seeded with status 'resolved'.
        mockMvc.perform(post("/cases/CS-10219/resolve").with(testJwt()))
                .andExpect(status().isConflict());

        verifyNoInteractions(governanceClient);
    }

    @Test
    void resolveReturns503WhenGovernanceMockUnavailable() throws Exception {
        given(governanceClient.recordAuditEvent(anyString(), anyString(), anyString(), anyString(), any()))
                .willThrow(new GovernanceUnavailableException("down", new RuntimeException()));

        mockMvc.perform(post("/cases/CS-10225/resolve").with(testJwt()))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void resolveRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/cases/CS-10225/resolve"))
                .andExpect(status().isUnauthorized());
    }
}
