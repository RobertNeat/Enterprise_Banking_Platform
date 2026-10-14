package robert_neat.banking_backend.collections;

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
        "spring.datasource.url=jdbc:h2:mem:collections_seed_on;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
class CollectionsControllerSeedOnTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsCollectionsCasesWithRequiredFields() throws Exception {
        mockMvc.perform(get("/collections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyOrNullString()))))
                .andExpect(jsonPath("$[*].customer").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyOrNullString()))))
                .andExpect(jsonPath("$[*].loanId").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyOrNullString()))))
                .andExpect(jsonPath("$[*].stage").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.in(
                        new String[] {"monitoring", "reminder", "settlement", "restructuring", "external", "legal"}))));
    }

    @Test
    void listsActionsForKnownCase() throws Exception {
        String caseId = com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(get("/collections")).andReturn().getResponse().getContentAsString(),
                "$[0].id");
        mockMvc.perform(get("/collections/" + caseId + "/actions"))
                .andExpect(status().isOk());
    }

    @Test
    void unknownCaseReturns404() throws Exception {
        mockMvc.perform(get("/collections/DBT-9999/actions"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
