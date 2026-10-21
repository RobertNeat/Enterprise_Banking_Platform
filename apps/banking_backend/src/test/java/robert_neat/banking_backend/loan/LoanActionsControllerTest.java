package robert_neat.banking_backend.loan;

import static org.mockito.ArgumentMatchers.any;
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

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.banking_backend.approvals.ApprovalRecord;
import robert_neat.banking_backend.approvals.GovernanceClient;
import robert_neat.banking_backend.approvals.GovernanceUnavailableException;
import robert_neat.banking_backend.creditrisk.CreditRiskClient;
import robert_neat.banking_backend.creditrisk.CreditRiskUnavailableException;
import robert_neat.banking_backend.creditrisk.CreditScoreResult;

/**
 * CreditRiskClient and GovernanceClient are mocked (@MockitoBean) — no real HTTP calls. testJwt()
 * defaults to subject PRC-00001 (Administrator systemu, seeded employee row).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "BANKING_SEED_MOCK_DATA=true",
      "spring.datasource.url=jdbc:h2:mem:loan_actions;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
    })
@Transactional
class LoanActionsControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CreditRiskClient creditRiskClient;

  @MockitoBean private GovernanceClient governanceClient;

  @Test
  void createScoresThroughCreditRiskMockAndSavesApplicationStage() throws Exception {
    given(creditRiskClient.score(eq("CUST-001"), any(BigDecimal.class), eq("retail")))
        .willReturn(
            new CreditScoreResult("CUST-001", 720, new BigDecimal("0.35"), "inq-1", Instant.now()));

    mockMvc
        .perform(
            post("/loans")
                .with(testJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"customerId":"CUST-001","segment":"retail","product":"cash",
                                 "requestedAmount":10000,"termMonths":12}"""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value("application"))
        .andExpect(jsonPath("$.scoreResult").value(720))
        .andExpect(jsonPath("$.dsti").value(0.35));
  }

  @Test
  void createReturns503WhenCreditRiskMockUnavailableAndDoesNotPersistAnything() throws Exception {
    given(creditRiskClient.score(anyString(), any(BigDecimal.class), anyString()))
        .willThrow(new CreditRiskUnavailableException("down", new RuntimeException()));

    mockMvc
        .perform(
            post("/loans")
                .with(testJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"customerId":"CUST-001","segment":"retail","product":"cash",
                                 "requestedAmount":10000,"termMonths":12}"""))
        .andExpect(status().isServiceUnavailable());

    // LN-80288 is the only seeded 'application'-stage loan; confirm no new row was persisted.
    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/loans")
                .with(testJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(13));
  }

  @Test
  void submitForApprovalMovesApplicationToDecisionAndRecordsAudit() throws Exception {
    // PRC-10234 (Doradca kredytowy, seniorityLevel 1) — not admin, has a checker available.
    given(
            governanceClient.createApproval(
                eq("LOAN_STAGE_TRANSITION"), eq("PRC-10234"), anyString()))
        .willReturn(
            new ApprovalRecord(
                "appr-loan-1",
                "LOAN_STAGE_TRANSITION",
                "PRC-10234",
                "payload",
                "PENDING",
                Instant.now(),
                null,
                null,
                null,
                null));

    // LN-80288 is seeded at stage 'application'.
    mockMvc
        .perform(post("/loans/LN-80288/submit-for-approval").with(testJwt("PRC-10234", 1)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.loan.stage").value("decision"))
        .andExpect(jsonPath("$.approvalId").value("appr-loan-1"));

    verify(governanceClient)
        .recordAuditEvent(
            eq("PRC-10234"),
            eq("LOAN_STAGE_CHANGED"),
            eq("LOAN_APPLICATION"),
            eq("LN-80288"),
            anyString());
  }

  @Test
  void submitForApprovalOnNonApplicationStageIsConflict() throws Exception {
    // LN-80231 is seeded at stage 'contract', not 'application'. Stage check must short-circuit
    // before the admin maker-checker check, so this stays a 409 even for the admin test JWT.
    mockMvc
        .perform(post("/loans/LN-80231/submit-for-approval").with(testJwt()))
        .andExpect(status().isConflict());

    verify(governanceClient, never()).createApproval(anyString(), anyString(), anyString());
  }

  @Test
  void submitForApprovalByAdminIsForbidden() throws Exception {
    // PRC-00001 is seeded as 'Administrator systemu' — the highest seniorityLevel in the
    // system, so no one could ever check this approval (see RoleCode.hasHigherSeniorityRole()).
    mockMvc
        .perform(post("/loans/LN-80288/submit-for-approval").with(testJwt()))
        .andExpect(status().isForbidden());

    verify(governanceClient, never()).createApproval(anyString(), anyString(), anyString());
  }

  @Test
  void submitForApprovalReturns503WhenGovernanceMockUnavailable() throws Exception {
    given(governanceClient.createApproval(anyString(), anyString(), anyString()))
        .willThrow(new GovernanceUnavailableException("down", new RuntimeException()));

    mockMvc
        .perform(post("/loans/LN-80288/submit-for-approval").with(testJwt("PRC-10234", 1)))
        .andExpect(status().isServiceUnavailable());
  }

  @Test
  void advanceStageRequiresApprovedGovernanceDecisionAndGeneratesInstallments() throws Exception {
    given(governanceClient.createApproval(anyString(), anyString(), anyString()))
        .willReturn(
            new ApprovalRecord(
                "appr-loan-2",
                "LOAN_STAGE_TRANSITION",
                "PRC-10234",
                "payload",
                "PENDING",
                Instant.now(),
                null,
                null,
                null,
                null));
    mockMvc
        .perform(post("/loans/LN-80288/submit-for-approval").with(testJwt("PRC-10234", 1)))
        .andExpect(status().isOk());

    given(governanceClient.getApproval("appr-loan-2"))
        .willReturn(
            new ApprovalRecord(
                "appr-loan-2",
                "LOAN_STAGE_TRANSITION",
                "PRC-10234",
                "payload",
                "APPROVED",
                Instant.now(),
                "PRC-00001",
                Instant.now(),
                null,
                null));

    mockMvc
        .perform(post("/loans/LN-80288/advance-stage").with(testJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value("contract"))
        .andExpect(jsonPath("$.installments.length()").value(36))
        .andExpect(jsonPath("$.approvedAmount").value(org.hamcrest.Matchers.notNullValue()));
  }

  @Test
  void advanceStageWhenApprovalStillPendingIsConflict() throws Exception {
    given(governanceClient.createApproval(anyString(), anyString(), anyString()))
        .willReturn(
            new ApprovalRecord(
                "appr-loan-3",
                "LOAN_STAGE_TRANSITION",
                "PRC-10234",
                "payload",
                "PENDING",
                Instant.now(),
                null,
                null,
                null,
                null));
    mockMvc
        .perform(post("/loans/LN-80288/submit-for-approval").with(testJwt("PRC-10234", 1)))
        .andExpect(status().isOk());

    given(governanceClient.getApproval("appr-loan-3"))
        .willReturn(
            new ApprovalRecord(
                "appr-loan-3",
                "LOAN_STAGE_TRANSITION",
                "PRC-10234",
                "payload",
                "PENDING",
                Instant.now(),
                null,
                null,
                null,
                null));

    mockMvc
        .perform(post("/loans/LN-80288/advance-stage").with(testJwt()))
        .andExpect(status().isConflict());
  }

  @Test
  void advanceStageOnRejectedApprovalReturnsLoanToApplicationStage() throws Exception {
    given(governanceClient.createApproval(anyString(), anyString(), anyString()))
        .willReturn(
            new ApprovalRecord(
                "appr-loan-4",
                "LOAN_STAGE_TRANSITION",
                "PRC-10234",
                "payload",
                "PENDING",
                Instant.now(),
                null,
                null,
                null,
                null));
    mockMvc
        .perform(post("/loans/LN-80288/submit-for-approval").with(testJwt("PRC-10234", 1)))
        .andExpect(status().isOk());

    given(governanceClient.getApproval("appr-loan-4"))
        .willReturn(
            new ApprovalRecord(
                "appr-loan-4",
                "LOAN_STAGE_TRANSITION",
                "PRC-10234",
                "payload",
                "REJECTED",
                Instant.now(),
                "PRC-00001",
                Instant.now(),
                null,
                null));

    mockMvc
        .perform(post("/loans/LN-80288/advance-stage").with(testJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value("application"))
        .andExpect(jsonPath("$.installments.length()").value(0));

    verify(governanceClient)
        .recordAuditEvent(
            eq("PRC-00001"),
            eq("LOAN_STAGE_CHANGED"),
            eq("LOAN_APPLICATION"),
            eq("LN-80288"),
            eq("decision -> application"));
  }

  @Test
  void loanCanBeResubmittedAfterReturningToApplicationFromRejection() throws Exception {
    given(governanceClient.createApproval(anyString(), anyString(), anyString()))
        .willReturn(
            new ApprovalRecord(
                "appr-loan-5",
                "LOAN_STAGE_TRANSITION",
                "PRC-10234",
                "payload",
                "PENDING",
                Instant.now(),
                null,
                null,
                null,
                null));
    mockMvc
        .perform(post("/loans/LN-80288/submit-for-approval").with(testJwt("PRC-10234", 1)))
        .andExpect(status().isOk());

    given(governanceClient.getApproval("appr-loan-5"))
        .willReturn(
            new ApprovalRecord(
                "appr-loan-5",
                "LOAN_STAGE_TRANSITION",
                "PRC-10234",
                "payload",
                "REJECTED",
                Instant.now(),
                "PRC-00001",
                Instant.now(),
                null,
                null));
    mockMvc
        .perform(post("/loans/LN-80288/advance-stage").with(testJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value("application"));

    // Loan is back at 'application' — submit-for-approval must accept it again.
    given(governanceClient.createApproval(anyString(), anyString(), anyString()))
        .willReturn(
            new ApprovalRecord(
                "appr-loan-6",
                "LOAN_STAGE_TRANSITION",
                "PRC-10234",
                "payload",
                "PENDING",
                Instant.now(),
                null,
                null,
                null,
                null));
    mockMvc
        .perform(post("/loans/LN-80288/submit-for-approval").with(testJwt("PRC-10234", 1)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.loan.stage").value("decision"))
        .andExpect(jsonPath("$.approvalId").value("appr-loan-6"));
  }

  @Test
  void disburseMovesContractLoanThroughDisbursementToScheduleAndRecordsTwoAudits()
      throws Exception {
    // LN-80231 is seeded at stage 'contract'.
    mockMvc
        .perform(post("/loans/LN-80231/disburse").with(testJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value("schedule"));

    verify(governanceClient)
        .recordAuditEvent(
            anyString(),
            eq("LOAN_STAGE_CHANGED"),
            eq("LOAN_APPLICATION"),
            eq("LN-80231"),
            eq("contract -> disbursement"));
    verify(governanceClient)
        .recordAuditEvent(
            anyString(),
            eq("LOAN_STAGE_CHANGED"),
            eq("LOAN_APPLICATION"),
            eq("LN-80231"),
            eq("disbursement -> schedule"));
  }

  @Test
  void disburseOnNonContractStageIsConflict() throws Exception {
    // LN-80288 is seeded at stage 'application', not 'contract'.
    mockMvc
        .perform(post("/loans/LN-80288/disburse").with(testJwt()))
        .andExpect(status().isConflict());

    verify(governanceClient, never())
        .recordAuditEvent(
            anyString(), eq("LOAN_STAGE_CHANGED"), anyString(), anyString(), anyString());
  }

  @Test
  void createWithInvalidSegmentIsBadRequestAndNeverCallsCreditRiskMock() throws Exception {
    mockMvc
        .perform(
            post("/loans")
                .with(testJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"customerId":"CUST-001","segment":"wholesale","product":"cash",
                                 "requestedAmount":10000,"termMonths":12}"""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("segment must be one of: retail, business"));

    verifyNoInteractions(creditRiskClient);
  }

  @Test
  void createWithInvalidProductIsBadRequestAndNeverCallsCreditRiskMock() throws Exception {
    mockMvc
        .perform(
            post("/loans")
                .with(testJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"customerId":"CUST-001","segment":"retail","product":"unknownProduct",
                                 "requestedAmount":10000,"termMonths":12}"""))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value(
                    "product must be one of: cash, mortgage, overdraft, investment, creditLine"));

    verifyNoInteractions(creditRiskClient);
  }

  @Test
  void createRequiresAuthentication() throws Exception {
    mockMvc
        .perform(post("/loans").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());
  }
}
