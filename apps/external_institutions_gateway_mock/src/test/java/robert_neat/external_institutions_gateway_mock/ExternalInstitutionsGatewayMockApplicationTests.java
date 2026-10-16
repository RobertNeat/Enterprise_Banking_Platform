package robert_neat.external_institutions_gateway_mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;
import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc zbudowany recznie z pelnego web context (nie @WebMvcTest/@AutoConfigureMockMvc),
 * zeby nie dociagac zaleznosci testowej poza spring-boot-starter-test (wzorzec z governance_mock).
 * gateway.submission.delay-ms skrocony w test properties, zeby testy async bylo szybkie.
 * Wszystkie metody testowe dzialaja na jednym, wspoldzielonym kontekscie Springa (jeden proces w pamieci),
 * wiec asercje licznosci/kolejnosci unikaja jsonPath length() i sprawdzaja konkretne id/wartosci.
 */
@SpringBootTest(properties = "gateway.submission.delay-ms=50")
class ExternalInstitutionsGatewayMockApplicationTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void uiPagesRenderSeededDataSuccessfully() throws Exception {
        // Seed one entity per service first, so th:each in the templates actually iterates a row
        // and record property access (id, xml, validationErrors) is exercised, not skipped on an empty list.
        mockMvc.perform(post("/enforcement/incoming")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"bailiff":"Komornik UI Test","caseNumber":"KM UI/01",
                         "customer":"CUST-UI","blockedAmount":111.00,"protectedAmount":11.00}
                        """));

        mockMvc.perform(post("/fatca-crs/export")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"reportId":"RPT-UI","period":"2025Q4","recipient":"FATCA/CRS",
                         "records":[{"customer":"Kowalski & Sons","taxId":"PL999","balance":10.00}]}
                        """));

        mockMvc.perform(post("/regulatory-gateway/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"reportId":"RPT-UI-SUB","recipient":"KNF","name":"Raport UI","period":"2025Q4"}
                        """));

        mockMvc.perform(get("/ui/enforcement"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("KM UI/01")));

        mockMvc.perform(get("/ui/reports"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("RPT-UI-SUB")));

        mockMvc.perform(get("/ui/fatca"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("RPT-UI")))
                // th:text escapes "&amp;" produced by the hand-built XML into "&amp;amp;" for HTML display.
                .andExpect(content().string(containsString("Kowalski &amp;amp; Sons")));

        mockMvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    void uiFormCreatesEnforcementCaseAndRedirects() throws Exception {
        mockMvc.perform(post("/ui/enforcement")
                        .param("bailiff", "Komornik Formularz")
                        .param("caseNumber", "KM FORM/01")
                        .param("customer", "CUST-FORM")
                        .param("blockedAmount", "250.00")
                        .param("protectedAmount", "50.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/ui/enforcement"));

        mockMvc.perform(get("/enforcement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].caseNumber", hasItem("KM FORM/01")));
    }

    @Test
    void enforcementIncomingListAndTransferLifecycle() throws Exception {
        String response = mockMvc.perform(post("/enforcement/incoming")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bailiff":"Komornik Jan Kowalski","caseNumber":"KM 123/26",
                                 "customer":"CUST-001","blockedAmount":5000.00,"protectedAmount":1200.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("registered"))
                .andReturn().getResponse().getContentAsString();
        String id = extractId(response);

        mockMvc.perform(get("/enforcement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(id)));

        mockMvc.perform(post("/enforcement/" + id + "/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transferredAmount\": 3800.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("settled"))
                .andExpect(jsonPath("$.confirmedAt").isNotEmpty());

        mockMvc.perform(post("/enforcement/" + id + "/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transferredAmount\": 100.00}"))
                .andExpect(status().isConflict());
    }

    @Test
    void transferOnUnknownCaseReturnsNotFound() throws Exception {
        mockMvc.perform(post("/enforcement/does-not-exist/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transferredAmount\": 100.00}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void incomingMissingFieldReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/enforcement/incoming")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bailiff\":\"X\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fatcaCrsExportGeneratesEscapedXml() throws Exception {
        mockMvc.perform(post("/fatca-crs/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reportId":"RPT-1","period":"2025Q4","recipient":"FATCA/CRS",
                                 "records":[{"customer":"Kowalski & Sons","taxId":"PL123","balance":1000.50}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value("RPT-1"))
                .andExpect(jsonPath("$.xml").value(containsString("Kowalski &amp; Sons")))
                .andExpect(jsonPath("$.xml").value(containsString("<FatcaCrsReport")));
    }

    @Test
    void fatcaCrsExportWithWrongRecipientReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/fatca-crs/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reportId":"RPT-2","period":"2025Q4","recipient":"KNF","records":[]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void regulatorySubmissionResolvesToAcceptedWhenPolled() throws Exception {
        String reportId = findReportIdWithHashMod10NotZero("REPORT-ACCEPT");

        String response = mockMvc.perform(post("/regulatory-gateway/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reportId":"%s","recipient":"KNF","name":"Raport kwartalny","period":"2025Q4"}
                                """.formatted(reportId)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        String submissionId = extractField(response, "submissionId");

        String resolvedBody = pollUntilResolved(submissionId);
        assertEquals("ACCEPTED", extractField(resolvedBody, "status"));
    }

    @Test
    void regulatorySubmissionDeterministicRejectionHasValidationErrors() throws Exception {
        String rejectingReportId = findReportIdWithHashMod10Zero("RPT-REJ");

        String response = mockMvc.perform(post("/regulatory-gateway/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reportId":"%s","recipient":"NBP","name":"Raport","period":"2025Q4"}
                                """.formatted(rejectingReportId)))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        String submissionId = extractField(response, "submissionId");

        String resolvedBody = pollUntilResolved(submissionId);
        assertEquals("REJECTED", extractField(resolvedBody, "status"));
        assertEquals(submissionId, extractField(resolvedBody, "submissionId"));
        assertThat(resolvedBody, containsString("validationErrors"));
        assertThat(resolvedBody, not(containsString("\"validationErrors\":[]")));
    }

    @Test
    void regulatorySubmissionWithInvalidRecipientReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/regulatory-gateway/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reportId":"RPT-3","recipient":"UNKNOWN","name":"Raport","period":"2025Q4"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUnknownSubmissionReturnsNotFound() throws Exception {
        mockMvc.perform(get("/regulatory-gateway/submissions/does-not-exist"))
                .andExpect(status().isNotFound());
    }

    private String findReportIdWithHashMod10Zero(String prefix) {
        for (int i = 0; i < 1000; i++) {
            String candidate = prefix + "-" + i;
            if (Math.floorMod(candidate.hashCode(), 10) == 0) {
                return candidate;
            }
        }
        throw new IllegalStateException("no candidate found");
    }

    private String findReportIdWithHashMod10NotZero(String prefix) {
        for (int i = 0; i < 1000; i++) {
            String candidate = prefix + "-" + i;
            if (Math.floorMod(candidate.hashCode(), 10) != 0) {
                return candidate;
            }
        }
        throw new IllegalStateException("no candidate found");
    }

    private String pollUntilResolved(String submissionId) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (Instant.now().isBefore(deadline)) {
            String body = mockMvc.perform(get("/regulatory-gateway/submissions/" + submissionId))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            if (!extractField(body, "status").equals("PENDING")) {
                return body;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Submission did not resolve in time: " + submissionId);
    }

    private String extractId(String json) {
        return extractField(json, "id");
    }

    private String extractField(String json, String field) {
        String marker = "\"" + field + "\":\"";
        int idx = json.indexOf(marker);
        int start = idx + marker.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }
}
