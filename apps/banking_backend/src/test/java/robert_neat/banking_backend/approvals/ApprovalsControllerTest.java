package robert_neat.banking_backend.approvals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static robert_neat.banking_backend.support.JwtTestSupport.testJwt;

/**
 * GovernanceClient is mocked (@MockitoBean) rather than hit over HTTP. Employee rows come from the
 * real mock-data seed (BANKING_SEED_MOCK_DATA=true): PRC-10234 = Doradca kredytowy (seniority 1),
 * PRC-50123 = Compliance / AML (seniority 2), PRC-00001 = Administrator systemu (seniority 3).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "BANKING_SEED_MOCK_DATA=true",
        "spring.datasource.url=jdbc:h2:mem:approvals_controller;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
class ApprovalsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GovernanceClient governanceClient;

    @Test
    void listReturnsApprovalsEnrichedWithRequesterRoleAndSeniority() throws Exception {
        given(governanceClient.listApprovals(null)).willReturn(List.of(
                new ApprovalRecord("appr-1", "LOAN_STAGE_TRANSITION", "PRC-10234", "payload",
                        "PENDING", Instant.now(), null, null, null, null)));

        mockMvc.perform(get("/approvals").with(testJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("appr-1"))
                .andExpect(jsonPath("$[0].requestedByRole").value("Doradca kredytowy"))
                .andExpect(jsonPath("$[0].requestedBySeniorityLevel").value(1));
    }

    @Test
    void listFiltersByStatusQueryParam() throws Exception {
        given(governanceClient.listApprovals("PENDING")).willReturn(List.of());

        mockMvc.perform(get("/approvals").param("status", "PENDING").with(testJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void decideByHigherSeniorityCheckerSucceedsAndSetsDecidedByFromJwtSubject() throws Exception {
        given(governanceClient.getApproval("appr-2")).willReturn(
                new ApprovalRecord("appr-2", "LOAN_STAGE_TRANSITION", "PRC-10234", "payload",
                        "PENDING", Instant.now(), null, null, null, null));
        given(governanceClient.decideApproval(eq("appr-2"), eq("APPROVED"), eq("PRC-00001"))).willReturn(
                new ApprovalRecord("appr-2", "LOAN_STAGE_TRANSITION", "PRC-10234", "payload",
                        "APPROVED", Instant.now(), "PRC-00001", Instant.now(), null, null));

        // PRC-00001 = admin, seniorityLevel 3, from the real mock-data seed.
        mockMvc.perform(post("/approvals/appr-2/decide")
                        .with(testJwt("PRC-00001", 3))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.decidedBy").value("PRC-00001"));

        verify(governanceClient).decideApproval("appr-2", "APPROVED", "PRC-00001");
    }

    @Test
    void decideByInsufficientSeniorityCheckerIsForbiddenAndNeverReachesGovernanceMock() throws Exception {
        given(governanceClient.getApproval("appr-3")).willReturn(
                new ApprovalRecord("appr-3", "LOAN_STAGE_TRANSITION", "PRC-10234", "payload",
                        "PENDING", Instant.now(), null, null, null, null));

        // PRC-30678 = Windykator (collections), seniorityLevel 1 — same as the requester's role
        // (loans, seniorityLevel 1), not strictly greater, so the checker rule must reject it.
        mockMvc.perform(post("/approvals/appr-3/decide")
                        .with(testJwt("PRC-30678", 1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isForbidden());

        verify(governanceClient, never()).decideApproval(any(), any(), any());
    }

    @Test
    void decideBySameEmployeeAsRequesterIsForbiddenEvenIfAdmin() throws Exception {
        given(governanceClient.getApproval("appr-4")).willReturn(
                new ApprovalRecord("appr-4", "LOAN_STAGE_TRANSITION", "PRC-00001", "payload",
                        "PENDING", Instant.now(), null, null, null, null));

        mockMvc.perform(post("/approvals/appr-4/decide")
                        .with(testJwt("PRC-00001", 3))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isForbidden());

        verify(governanceClient, never()).decideApproval(any(), any(), any());
    }

    @Test
    void decideWithUnresolvableRequesterRoleIsForbidden() throws Exception {
        // PRC-60001 exists in the seed but with role "Doradca klienta", which is not in RoleCode —
        // the maker-checker gate must fail closed rather than silently allow the decision.
        given(governanceClient.getApproval("appr-5")).willReturn(
                new ApprovalRecord("appr-5", "SOME_TYPE", "PRC-60001", "payload",
                        "PENDING", Instant.now(), null, null, null, null));

        mockMvc.perform(post("/approvals/appr-5/decide")
                        .with(testJwt("PRC-00001", 3))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isForbidden());

        verify(governanceClient, never()).decideApproval(any(), any(), any());
    }

    @Test
    void getByIdRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/approvals/appr-1"))
                .andExpect(status().isUnauthorized());
    }
}
