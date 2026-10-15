package robert_neat.banking_backend.collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import robert_neat.banking_backend.approvals.GovernanceClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static robert_neat.banking_backend.support.JwtTestSupport.testJwt;

/**
 * GovernanceClient is mocked (@MockitoBean). Seed: DBT-6601 stage 'legal', DBT-6580 stage
 * 'monitoring', DBT-6572 stage 'restructuring' — see db/changelog/seed/008-collections.sql.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "BANKING_SEED_MOCK_DATA=true",
        "spring.datasource.url=jdbc:h2:mem:collections_actions;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
@Transactional
class CollectionsActionsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GovernanceClient governanceClient;

    @Test
    void remindOnLegalStageIsConflict() throws Exception {
        mockMvc.perform(post("/collections/DBT-6601/remind").with(testJwt()))
                .andExpect(status().isConflict());

        verify(governanceClient, org.mockito.Mockito.never())
                .recordAuditEvent(anyString(), eq("COLLECTIONS_REMINDER_SENT"), anyString(), anyString(), any());
    }

    @Test
    void remindOnNonLegalStageRecordsAuditWithoutStageChange() throws Exception {
        given(governanceClient.recordAuditEvent(anyString(), anyString(), anyString(), anyString(), any()))
                .willReturn("audit-1");

        mockMvc.perform(post("/collections/DBT-6580/remind").with(testJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("monitoring"));

        verify(governanceClient).recordAuditEvent(eq("PRC-00001"), eq("COLLECTIONS_REMINDER_SENT"),
                eq("COLLECTIONS_CASE"), eq("DBT-6580"), any());
    }

    @Test
    void proposeSettlementMovesStageToSettlement() throws Exception {
        given(governanceClient.recordAuditEvent(anyString(), anyString(), anyString(), anyString(), any()))
                .willReturn("audit-2");

        mockMvc.perform(post("/collections/DBT-6580/propose-settlement").with(testJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("settlement"));
    }

    @Test
    void transferExternalOnMonitoringStageIsConflict() throws Exception {
        mockMvc.perform(post("/collections/DBT-6580/transfer-external").with(testJwt()))
                .andExpect(status().isConflict());
    }

    @Test
    void transferExternalOnEligibleStageMovesToExternal() throws Exception {
        given(governanceClient.recordAuditEvent(anyString(), anyString(), anyString(), anyString(), any()))
                .willReturn("audit-3");

        mockMvc.perform(post("/collections/DBT-6572/transfer-external").with(testJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("external"));
    }

    @Test
    void remindRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/collections/DBT-6580/remind"))
                .andExpect(status().isUnauthorized());
    }
}
