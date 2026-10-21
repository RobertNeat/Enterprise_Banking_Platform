package robert_neat.governance_mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc is built manually from the full web context rather than via @WebMvcTest/@AutoConfigureMockMvc,
 * to avoid pulling in a test-module dependency beyond spring-boot-starter-test. Request/response
 * bodies are plain JSON strings asserted via jsonPath, avoiding any direct Jackson import.
 */
@SpringBootTest
class GovernanceMockApplicationTests {

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
    void approvalDecideBySameRequesterIsRejectedWithConflict() throws Exception {
        String approvalId = createApproval("LOAN_STAGE_TRANSITION", "maker-1", "move to decision stage");

        mockMvc.perform(post("/approvals/" + approvalId + "/decide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\",\"decidedBy\":\"maker-1\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/audit-events").param("entityType", "APPROVAL").param("entityId", approvalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void approvalDecideByDifferentCheckerSucceedsAndWritesSingleAuditEvent() throws Exception {
        String approvalId = createApproval("ENFORCEMENT_TRANSFER", "maker-2", "transfer to bailiff");

        mockMvc.perform(post("/approvals/" + approvalId + "/decide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\",\"decidedBy\":\"checker-9\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.decidedBy").value("checker-9"));

        mockMvc.perform(get("/audit-events").param("entityType", "APPROVAL").param("entityId", approvalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].action").value("APPROVAL_APPROVED"))
                .andExpect(jsonPath("$[0].actor").value("checker-9"));
    }

    @Test
    void decidingAlreadyDecidedApprovalReturnsConflict() throws Exception {
        String approvalId = createApproval("LOAN_STAGE_TRANSITION", "maker-3", "payload");

        mockMvc.perform(post("/approvals/" + approvalId + "/decide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"REJECTED\",\"decidedBy\":\"checker-1\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/approvals/" + approvalId + "/decide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\",\"decidedBy\":\"checker-2\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void decidingUnknownApprovalReturnsNotFound() throws Exception {
        mockMvc.perform(post("/approvals/does-not-exist/decide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\",\"decidedBy\":\"checker-1\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void slaCheckBelowZeroEscalatesAndWritesAuditEvent() throws Exception {
        String caseId = "case-sla-neg";

        mockMvc.perform(post("/cases/" + caseId + "/sla-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slaRemainingHours\": -1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.escalated").value(true))
                .andExpect(jsonPath("$.auditEventId").isNotEmpty());

        mockMvc.perform(get("/audit-events").param("entityType", "CUSTOMER_CASE").param("entityId", caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].action").value("AUTO_ESCALATED"));
    }

    @Test
    void slaCheckAboveZeroDoesNotEscalateOrWriteAuditEvent() throws Exception {
        String caseId = "case-sla-pos";

        mockMvc.perform(post("/cases/" + caseId + "/sla-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slaRemainingHours\": 5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.escalated").value(false))
                .andExpect(jsonPath("$.auditEventId").isEmpty());

        mockMvc.perform(get("/audit-events").param("entityType", "CUSTOMER_CASE").param("entityId", caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void slaCheckMissingFieldReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/cases/case-missing-field/sla-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ictIncidentLifecycleCreateAndResolve() throws Exception {
        String response = mockMvc.perform(post("/ict-incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"CARD_PROCESSOR\",\"description\":\"outage\",\"severity\":\"HIGH\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();

        String incidentId = extractId(response);

        mockMvc.perform(post("/ict-incidents/" + incidentId + "/resolve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        mockMvc.perform(post("/ict-incidents/" + incidentId + "/resolve"))
                .andExpect(status().isConflict());
    }

    @Test
    void ictIncidentsListFiltersByStatusAndSeverity() throws Exception {
        String openHighId = extractId(mockMvc.perform(post("/ict-incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"CARD_PROCESSOR\",\"description\":\"outage\",\"severity\":\"HIGH\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        String resolvedLowId = extractId(mockMvc.perform(post("/ict-incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"PAYMENT_GATEWAY\",\"description\":\"delay\",\"severity\":\"LOW\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        mockMvc.perform(post("/ict-incidents/" + resolvedLowId + "/resolve"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/ict-incidents").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + openHighId + "')]").exists())
                .andExpect(jsonPath("$[?(@.id=='" + resolvedLowId + "')]").doesNotExist());

        mockMvc.perform(get("/ict-incidents").param("severity", "LOW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + resolvedLowId + "')]").exists())
                .andExpect(jsonPath("$[?(@.id=='" + openHighId + "')]").doesNotExist());

        mockMvc.perform(get("/ict-incidents").param("status", "OPEN").param("severity", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + openHighId + "')]").exists())
                .andExpect(jsonPath("$[?(@.id=='" + resolvedLowId + "')]").doesNotExist());

        mockMvc.perform(get("/ict-incidents").param("status", "OPEN").param("severity", "LOW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + openHighId + "')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.id=='" + resolvedLowId + "')]").doesNotExist());
    }

    @Test
    void createApprovalMissingTypeReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/approvals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestedBy\":\"maker-1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createIncidentWithInvalidSeverityReturnsProblemDetail() throws Exception {
        mockMvc.perform(post("/ict-incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"CARD_PROCESSOR\",\"description\":\"outage\",\"severity\":\"CRITICAL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void listIctIncidentsWithUnrecognizedStatusReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/ict-incidents").param("status", "OPENED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void listIctIncidentsWithUnrecognizedSeverityReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/ict-incidents").param("severity", "CRITICAL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void listIctIncidentsWithLowercaseSeverityReturnsBadRequest() throws Exception {
        // Enum query-param binding is case-sensitive by default (same as GET /approvals?status=...);
        // only the exact enum constant name matches.
        mockMvc.perform(get("/ict-incidents").param("severity", "low"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private String createApproval(String type, String requestedBy, String payload) throws Exception {
        String body = """
                {"type":"%s","requestedBy":"%s","payload":"%s"}
                """.formatted(type, requestedBy, payload);
        String response = mockMvc.perform(post("/approvals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return extractId(response);
    }

    private String extractId(String json) {
        int idx = json.indexOf("\"id\":\"");
        int start = idx + 6;
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }
}
