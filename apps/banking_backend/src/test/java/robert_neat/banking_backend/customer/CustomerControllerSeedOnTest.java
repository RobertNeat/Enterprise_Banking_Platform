package robert_neat.banking_backend.customer;

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
        "spring.datasource.url=jdbc:h2:mem:customer_seed_on;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
class CustomerControllerSeedOnTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsCustomers() throws Exception {
        mockMvc.perform(get("/customers").with(testJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12))
                .andExpect(jsonPath("$[0].fullName").isNotEmpty());
    }

    @Test
    void listsEmployeesWithBusinessLinesSplit() throws Exception {
        mockMvc.perform(get("/employees").with(testJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$[0].businessLines").isArray());
    }

    @Test
    void listsAccountsForKnownCustomer() throws Exception {
        mockMvc.perform(get("/customers/CUST-001/accounts").with(testJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$[0].maskedNumber").isNotEmpty());
    }

    @Test
    void unknownCustomerAccountsReturns404() throws Exception {
        mockMvc.perform(get("/customers/CUST-999/accounts").with(testJwt())).andExpect(status().isNotFound());
    }
}
