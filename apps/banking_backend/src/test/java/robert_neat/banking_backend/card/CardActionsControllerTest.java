package robert_neat.banking_backend.card;

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
 * CardProcessorClient and GovernanceClient are mocked (@MockitoBean) — no real HTTP calls.
 * testJwt() defaults to subject PRC-00001 (Administrator systemu, seeded employee row). Seed:
 * CRD-5521 active, CRD-5522 blocked, CRD-5541 pending, CRD-5498 expired (see
 * db/changelog/seed/003-card.sql).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "BANKING_SEED_MOCK_DATA=true",
        "spring.datasource.url=jdbc:h2:mem:card_actions;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
@Transactional
class CardActionsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardProcessorClient cardProcessorClient;

    @MockitoBean
    private GovernanceClient governanceClient;

    @Test
    void blockActiveCardUpdatesStatusAndRecordsAudit() throws Exception {
        given(cardProcessorClient.blockCard("CRD-5521")).willReturn(new CardBlockStatusResponse("CRD-5521", true));

        mockMvc.perform(post("/cards/CRD-5521/block").with(testJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("blocked"));

        verify(governanceClient).recordAuditEvent(eq("PRC-00001"), eq("CARD_BLOCKED"),
                eq("PAYMENT_CARD"), eq("CRD-5521"), any());
    }

    @Test
    void blockAlreadyBlockedCardIsConflict() throws Exception {
        mockMvc.perform(post("/cards/CRD-5522/block").with(testJwt()))
                .andExpect(status().isConflict());

        verify(cardProcessorClient, never()).blockCard(anyString());
    }

    @Test
    void blockPendingCardIsConflict() throws Exception {
        mockMvc.perform(post("/cards/CRD-5541/block").with(testJwt()))
                .andExpect(status().isConflict());
    }

    @Test
    void blockExpiredCardIsConflict() throws Exception {
        mockMvc.perform(post("/cards/CRD-5498/block").with(testJwt()))
                .andExpect(status().isConflict());
    }

    @Test
    void unblockBlockedCardUpdatesStatusAndRecordsAudit() throws Exception {
        given(cardProcessorClient.unblockCard("CRD-5522"))
                .willReturn(new CardBlockStatusResponse("CRD-5522", false));

        mockMvc.perform(post("/cards/CRD-5522/unblock").with(testJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("active"));

        verify(governanceClient).recordAuditEvent(eq("PRC-00001"), eq("CARD_UNBLOCKED"),
                eq("PAYMENT_CARD"), eq("CRD-5522"), any());
    }

    @Test
    void unblockActiveCardIsConflict() throws Exception {
        mockMvc.perform(post("/cards/CRD-5521/unblock").with(testJwt()))
                .andExpect(status().isConflict());

        verify(cardProcessorClient, never()).unblockCard(anyString());
    }

    @Test
    void blockReturns503AndRecordsIctIncidentWhenProcessorUnavailable() throws Exception {
        given(cardProcessorClient.blockCard("CRD-5521"))
                .willThrow(new CardProcessorUnavailableException("outage simulated"));

        mockMvc.perform(post("/cards/CRD-5521/block").with(testJwt()))
                .andExpect(status().isServiceUnavailable());

        verify(governanceClient).createIctIncident(eq("CARD_PROCESSOR"), anyString(), eq("HIGH"));
        verify(governanceClient, never()).recordAuditEvent(anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void blockReturns503EvenWhenGovernanceAlsoUnavailable() throws Exception {
        given(cardProcessorClient.blockCard("CRD-5521"))
                .willThrow(new CardProcessorUnavailableException("outage simulated"));
        org.mockito.Mockito.doThrow(new RuntimeException("governance_mock also down"))
                .when(governanceClient).createIctIncident(anyString(), anyString(), anyString());

        mockMvc.perform(post("/cards/CRD-5521/block").with(testJwt()))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void fileChargebackPersistsRefAndRecordsAudit() throws Exception {
        // dfd92a8d-... is seeded for CRD-5521 (see db/changelog/seed/003-card.sql).
        given(cardProcessorClient.fileChargeback("dfd92a8d-976a-4601-97be-13ab2e09c7b1", "4837", "opis"))
                .willReturn(new CardProcessorChargebackResponse(
                        "new-cb-1", "dfd92a8d-976a-4601-97be-13ab2e09c7b1", "4837", "opis", "FILED", Instant.now()));

        mockMvc.perform(post("/cards/CRD-5521/chargebacks").with(testJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authorizationId":"dfd92a8d-976a-4601-97be-13ab2e09c7b1",
                                 "reasonCode":"4837","description":"opis"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("new-cb-1"))
                .andExpect(jsonPath("$.status").value("FILED"));

        verify(governanceClient).recordAuditEvent(eq("PRC-00001"), eq("CARD_CHARGEBACK_FILED"),
                eq("CHARGEBACK"), eq("new-cb-1"), any());
    }

    @Test
    void fileChargebackForAuthorizationOfAnotherCardIsNotFound() throws Exception {
        // dfd92a8d-... belongs to CRD-5521, not CRD-5522.
        mockMvc.perform(post("/cards/CRD-5522/chargebacks").with(testJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authorizationId":"dfd92a8d-976a-4601-97be-13ab2e09c7b1",
                                 "reasonCode":"4837","description":"opis"}"""))
                .andExpect(status().isNotFound());

        verify(cardProcessorClient, never()).fileChargeback(anyString(), anyString(), anyString());
    }

    @Test
    void advanceChargebackUpdatesStatusAndRecordsAudit() throws Exception {
        // d9a633a4-... is seeded FILED (see db/changelog/seed/003-card.sql).
        given(cardProcessorClient.advanceChargeback("d9a633a4-1223-48cd-b470-217a69610114", "UNDER_REVIEW"))
                .willReturn(new CardProcessorChargebackResponse("d9a633a4-1223-48cd-b470-217a69610114",
                        "449084f7-77b5-4cab-a790-e680763a6572", "4837", "opis", "UNDER_REVIEW", Instant.now()));

        mockMvc.perform(post("/cards/chargebacks/d9a633a4-1223-48cd-b470-217a69610114/advance").with(testJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"UNDER_REVIEW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        verify(governanceClient).recordAuditEvent(eq("PRC-00001"), eq("CARD_CHARGEBACK_ADVANCED"),
                eq("CHARGEBACK"), eq("d9a633a4-1223-48cd-b470-217a69610114"), eq("UNDER_REVIEW"));
    }

    @Test
    void advanceUnknownChargebackIsNotFound() throws Exception {
        mockMvc.perform(post("/cards/chargebacks/unknown-id/advance").with(testJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"UNDER_REVIEW\"}"))
                .andExpect(status().isNotFound());

        verify(cardProcessorClient, never()).advanceChargeback(anyString(), anyString());
    }

    @Test
    void blockRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/cards/CRD-5521/block"))
                .andExpect(status().isUnauthorized());
    }
}
