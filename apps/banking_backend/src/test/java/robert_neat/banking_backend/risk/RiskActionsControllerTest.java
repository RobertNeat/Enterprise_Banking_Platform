package robert_neat.banking_backend.risk;

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
import robert_neat.banking_backend.creditrisk.CreditRiskClient;
import robert_neat.banking_backend.creditrisk.CreditRiskUnavailableException;
import robert_neat.banking_backend.creditrisk.EclClassificationResult;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static robert_neat.banking_backend.support.JwtTestSupport.testJwt;

/**
 * CreditRiskClient and GovernanceClient are mocked (@MockitoBean) — no real HTTP calls.
 * Seed: COL-3301 (stage1, rating BBB, valuation 620000) — see db/changelog/seed/007-risk.sql.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "BANKING_SEED_MOCK_DATA=true",
        "spring.datasource.url=jdbc:h2:mem:risk_actions;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
@Transactional
class RiskActionsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreditRiskClient creditRiskClient;

    @MockitoBean
    private GovernanceClient governanceClient;

    @Test
    void reclassifyUpdatesStageProvisionAndRecordsAudit() throws Exception {
        given(creditRiskClient.classifyEcl(eq("COL-3301"), eq(45), eq("BBB"), any(BigDecimal.class)))
                .willReturn(new EclClassificationResult("COL-3301", "stage2", new BigDecimal("31000.00"), Instant.now()));

        mockMvc.perform(post("/risk/collateral/COL-3301/reclassify").with(testJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"daysPastDue\":45}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("stage2"))
                .andExpect(jsonPath("$.provision").value(31000.00));

        verify(governanceClient).recordAuditEvent(eq("PRC-00001"), eq("COLLATERAL_RECLASSIFIED"),
                eq("COLLATERAL_ENTRY"), eq("COL-3301"), any());
    }

    @Test
    void reclassifyUnknownEntryIsNotFound() throws Exception {
        mockMvc.perform(post("/risk/collateral/COL-9999/reclassify").with(testJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"daysPastDue\":10}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void reclassifyReturns503WhenCreditRiskMockUnavailable() throws Exception {
        given(creditRiskClient.classifyEcl(anyString(), anyInt(), anyString(), any(BigDecimal.class)))
                .willThrow(new CreditRiskUnavailableException("down", new RuntimeException()));

        mockMvc.perform(post("/risk/collateral/COL-3301/reclassify").with(testJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"daysPastDue\":10}"))
                .andExpect(status().isServiceUnavailable());
    }
}
