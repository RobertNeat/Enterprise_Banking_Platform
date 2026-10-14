package robert_neat.banking_backend.loan;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "BANKING_SEED_MOCK_DATA=false",
        "spring.datasource.url=jdbc:h2:mem:loans_seed_off;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
class LoanControllerSeedOffTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listReturnsEmptyArrayWhenNotSeeded() throws Exception {
        mockMvc.perform(get("/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void detailReturns404WhenNotSeeded() throws Exception {
        mockMvc.perform(get("/loans/LN-80231"))
                .andExpect(status().isNotFound());
    }
}
