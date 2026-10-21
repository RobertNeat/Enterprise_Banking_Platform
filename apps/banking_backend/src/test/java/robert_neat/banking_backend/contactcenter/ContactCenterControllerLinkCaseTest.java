package robert_neat.banking_backend.contactcenter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static robert_neat.banking_backend.support.JwtTestSupport.testJwt;

/**
 * KKT — PUT /contact-center/dispositions/{id}/case. Own test class (own H2 instance, no class-level
 * @Transactional) so the mutation does not leak into ContactCenterControllerSeedOnTest's shared
 * database and its count assertions, and so the re-GET in the first test proves the write was
 * actually committed rather than just visible within one shared test transaction.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "BANKING_SEED_MOCK_DATA=true",
        "spring.datasource.url=jdbc:h2:mem:contactcenter_link_case;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
class ContactCenterControllerLinkCaseTest {

    @Autowired
    private MockMvc mockMvc;

    /** DSP-771 is seeded linked to CS-10229 (CUST-001). Creates a 2nd CUST-001 case to relink to. */
    @Test
    void linksDispositionToAnotherCaseOfTheSameCustomer() throws Exception {
        String newCaseId = createCase("CUST-001");

        mockMvc.perform(put("/contact-center/dispositions/DSP-771/case").with(testJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"caseId\":\"" + newCaseId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("DSP-771"))
                .andExpect(jsonPath("$.linkedCase").value(newCaseId));

        // re-GET to confirm the write was actually persisted (class-level @Transactional(readOnly) risk).
        // DSP-771 is CUST-001's only seeded disposition, so a single-element filtered result is expected.
        mockMvc.perform(get("/contact-center/dispositions").with(testJwt()).param("customerId", "CUST-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("DSP-771"))
                .andExpect(jsonPath("$[0].linkedCase").value(newCaseId));
    }

    @Test
    void linkingToUnknownCaseIs404() throws Exception {
        mockMvc.perform(put("/contact-center/dispositions/DSP-771/case").with(testJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"caseId\":\"CS-99999\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void linkingUnknownDispositionIs404() throws Exception {
        mockMvc.perform(put("/contact-center/dispositions/DSP-999/case").with(testJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"caseId\":\"CS-10229\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void linkingToBlankCaseIdIsBadRequest() throws Exception {
        mockMvc.perform(put("/contact-center/dispositions/DSP-771/case").with(testJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"caseId\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    /** CS-10231 belongs to CUST-009 — DSP-771 belongs to CUST-001's CS-10229. Cross-customer link rejected. */
    @Test
    void linkingToAnotherCustomersCaseIsBadRequest() throws Exception {
        mockMvc.perform(put("/contact-center/dispositions/DSP-771/case").with(testJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"caseId\":\"CS-10231\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void linkCaseRequiresAuthentication() throws Exception {
        mockMvc.perform(put("/contact-center/dispositions/DSP-771/case")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"caseId\":\"CS-10229\"}"))
                .andExpect(status().isUnauthorized());
    }

    private String createCase(String customerId) throws Exception {
        var result = mockMvc.perform(post("/cases").with(testJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Test case\",\"customerId\":\"" + customerId + "\","
                                + "\"priority\":\"low\",\"caseType\":\"inquiry\"}"))
                .andExpect(status().isOk())
                .andReturn();
        tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
