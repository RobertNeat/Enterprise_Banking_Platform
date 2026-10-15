package robert_neat.banking_backend.enforcement;

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
import robert_neat.banking_backend.reporting.EnforcementIncomingResponse;
import robert_neat.banking_backend.reporting.EnforcementTransferResponse;
import robert_neat.banking_backend.reporting.RegulatoryGatewayClient;
import robert_neat.banking_backend.reporting.RegulatoryGatewayUnavailableException;

import java.math.BigDecimal;
import java.time.Instant;

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
 * RegulatoryGatewayClient and GovernanceClient are mocked (@MockitoBean) — no real HTTP calls.
 * Seed: EN-2205 'registered' (gatewayEnforcementId NULL), EN-2201 'active' (gatewayEnforcementId
 * NULL, seeded — no gateway linkage), EN-2198 'settled' — see db/changelog/seed/009-enforcement.sql.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "BANKING_SEED_MOCK_DATA=true",
        "spring.datasource.url=jdbc:h2:mem:enforcement_actions;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
@Transactional
class EnforcementActionsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegulatoryGatewayClient gatewayClient;

    @MockitoBean
    private GovernanceClient governanceClient;

    @Test
    void registerCreatesCaseWithGatewayLinkageAndRecordsAudit() throws Exception {
        given(gatewayClient.registerEnforcement(anyString(), anyString(), eq("CUST-001"), any(BigDecimal.class), any()))
                .willReturn(new EnforcementIncomingResponse(
                        "gw-uuid-1", "CUST-001", "Komornik Jan Kowalski", "KM 1/26",
                        new BigDecimal("5000.00"), null, new BigDecimal("1200.00"), "registered",
                        Instant.now(), null));

        mockMvc.perform(post("/enforcement").with(testJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bailiff":"Komornik Jan Kowalski","caseNumber":"KM 1/26","customer":"CUST-001",
                                 "blockedAmount":5000.00,"protectedAmount":1200.00}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("registered"))
                .andExpect(jsonPath("$.blockedAmount").value(5000.00));

        verify(governanceClient).recordAuditEvent(eq("PRC-00001"), eq("ENFORCEMENT_REGISTERED"),
                eq("ENFORCEMENT_CASE"), anyString(), any());
    }

    @Test
    void registerReturns503AndRecordsIctIncidentWhenGatewayUnavailable() throws Exception {
        given(gatewayClient.registerEnforcement(anyString(), anyString(), anyString(), any(BigDecimal.class), any()))
                .willThrow(new RegulatoryGatewayUnavailableException("down", new RuntimeException()));

        mockMvc.perform(post("/enforcement").with(testJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bailiff":"Komornik","caseNumber":"KM 2/26","customer":"CUST-001",
                                 "blockedAmount":1000.00}"""))
                .andExpect(status().isServiceUnavailable());

        verify(governanceClient).createIctIncident(eq("EXTERNAL_INSTITUTIONS_GATEWAY"), anyString(), eq("HIGH"));
    }

    @Test
    void requestTransferApprovalOnSeededRegisteredCaseWithoutLinkageIsConflict() throws Exception {
        // EN-2205 is seeded 'registered' with gatewayEnforcementId=NULL.
        mockMvc.perform(post("/enforcement/EN-2205/request-transfer-approval").with(testJwt()))
                .andExpect(status().isConflict());

        verify(governanceClient, never()).createApproval(anyString(), anyString(), anyString());
    }

    @Test
    void transferOnSeededActiveCaseWithoutLinkageIsConflict() throws Exception {
        // EN-2201 is seeded 'active' with gatewayEnforcementId=NULL (no gateway linkage).
        mockMvc.perform(post("/enforcement/EN-2201/transfer").with(testJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transferredAmount\":100.00}"))
                .andExpect(status().isConflict());

        verify(gatewayClient, never()).transferEnforcement(anyString(), any());
    }

    @Test
    void fullMakerCheckerFlowRegistersRequestsApprovalAndTransfers() throws Exception {
        given(gatewayClient.registerEnforcement(anyString(), anyString(), eq("CUST-001"), any(BigDecimal.class), any()))
                .willReturn(new EnforcementIncomingResponse(
                        "gw-uuid-2", "CUST-001", "Komornik Jan Kowalski", "KM 3/26",
                        new BigDecimal("5000.00"), null, new BigDecimal("1200.00"), "registered",
                        Instant.now(), null));

        String body = mockMvc.perform(post("/enforcement").with(testJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bailiff":"Komornik Jan Kowalski","caseNumber":"KM 3/26","customer":"CUST-001",
                                 "blockedAmount":5000.00,"protectedAmount":1200.00}"""))
                .andReturn().getResponse().getContentAsString();
        String caseId = com.jayway.jsonpath.JsonPath.read(body, "$.id");

        given(governanceClient.createApproval(eq("ENFORCEMENT_TRANSFER"), eq("PRC-00001"), anyString()))
                .willReturn(new ApprovalRecord("appr-enf-1", "ENFORCEMENT_TRANSFER", "PRC-00001", "payload",
                        "PENDING", Instant.now(), null, null, null, null));

        mockMvc.perform(post("/enforcement/" + caseId + "/request-transfer-approval").with(testJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enforcementCase.status").value("active"))
                .andExpect(jsonPath("$.approvalId").value("appr-enf-1"));

        // Approval still PENDING -> transfer is rejected.
        given(governanceClient.getApproval("appr-enf-1")).willReturn(
                new ApprovalRecord("appr-enf-1", "ENFORCEMENT_TRANSFER", "PRC-00001", "payload",
                        "PENDING", Instant.now(), null, null, null, null));
        mockMvc.perform(post("/enforcement/" + caseId + "/transfer").with(testJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transferredAmount\":4800.00}"))
                .andExpect(status().isConflict());

        // Approval now APPROVED -> transfer succeeds.
        given(governanceClient.getApproval("appr-enf-1")).willReturn(
                new ApprovalRecord("appr-enf-1", "ENFORCEMENT_TRANSFER", "PRC-00001", "payload",
                        "APPROVED", Instant.now(), "PRC-50123", Instant.now(), null, null));
        given(gatewayClient.transferEnforcement(eq("gw-uuid-2"), eq(new BigDecimal("4800.00"))))
                .willReturn(new EnforcementTransferResponse("gw-uuid-2", "settled", Instant.now()));

        mockMvc.perform(post("/enforcement/" + caseId + "/transfer").with(testJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transferredAmount\":4800.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("settled"))
                .andExpect(jsonPath("$.transferredAmount").value(4800.00));

        verify(governanceClient).recordAuditEvent(eq("PRC-00001"), eq("ENFORCEMENT_FUNDS_TRANSFERRED"),
                eq("ENFORCEMENT_CASE"), eq(caseId), any());
    }

    @Test
    void registerRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/enforcement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
