package robert_neat.banking_backend.reporting;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "BANKING_SEED_MOCK_DATA=true",
        "spring.datasource.url=jdbc:h2:mem:reporting_seed_on;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
class ReportingControllerSeedOnTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsReportsWithAllowedStatuses() throws Exception {
        mockMvc.perform(get("/reporting/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyOrNullString()))))
                .andExpect(jsonPath("$[*].status").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.in(new String[] {"draft", "submitted", "accepted"}))));
    }

    @Test
    void listsSubmissionsForKnownReport() throws Exception {
        mockMvc.perform(get("/reporting/reports/RPT-NBP-014/submissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void unknownReportReturns404() throws Exception {
        mockMvc.perform(get("/reporting/reports/RPT-XXX-999/submissions"))
                .andExpect(status().isNotFound());
    }
}
