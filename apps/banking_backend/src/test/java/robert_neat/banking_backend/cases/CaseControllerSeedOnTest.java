package robert_neat.banking_backend.cases;

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
        "spring.datasource.url=jdbc:h2:mem:cases_seed_on;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
class CaseControllerSeedOnTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsCasesWithRequiredFieldsAndAllowedStatuses() throws Exception {
        mockMvc.perform(get("/cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyOrNullString()))))
                .andExpect(jsonPath("$[*].subject").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyOrNullString()))))
                .andExpect(jsonPath("$[*].customer").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyOrNullString()))))
                .andExpect(jsonPath("$[*].owner").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyOrNullString()))))
                .andExpect(jsonPath("$[*].status").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.in(new String[] {"new", "inProgress", "escalated", "resolved"}))));
    }

    @Test
    void ownerIsFormattedAsInitialAndLastName() throws Exception {
        mockMvc.perform(get("/cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='CS-10231')].owner").value(
                        org.hamcrest.Matchers.contains("A. Kowalska")));
    }
}
