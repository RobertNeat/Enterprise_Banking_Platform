package robert_neat.banking_backend.contactcenter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static robert_neat.banking_backend.support.JwtTestSupport.testJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "BANKING_SEED_MOCK_DATA=true",
        "spring.datasource.url=jdbc:h2:mem:contactcenter_seed_on;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
class ContactCenterControllerSeedOnTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsDispositionsWithRequiredFields() throws Exception {
        mockMvc.perform(get("/contact-center/dispositions").with(testJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyOrNullString()))))
                .andExpect(jsonPath("$[*].linkedCase").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyOrNullString()))))
                .andExpect(jsonPath("$[*].status").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.in(
                        new String[] {"new", "inProgress", "escalated", "resolved"}))));
    }

    @Test
    void listsHistoryWithRequiredFields() throws Exception {
        mockMvc.perform(get("/contact-center/history").with(testJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].summary").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyOrNullString()))));
    }

    @Test
    void profileAggregatesCustomerData() throws Exception {
        mockMvc.perform(get("/contact-center/profile/CUST-001").with(testJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jan Wiśniewski"))
                .andExpect(jsonPath("$.riskRating").isNotEmpty());
    }

    @Test
    void unknownCustomerProfileReturns404() throws Exception {
        mockMvc.perform(get("/contact-center/profile/CUST-999").with(testJwt()))
                .andExpect(status().isNotFound());
    }
}
