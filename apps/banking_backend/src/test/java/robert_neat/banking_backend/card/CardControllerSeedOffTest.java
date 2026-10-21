package robert_neat.banking_backend.card;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static robert_neat.banking_backend.support.JwtTestSupport.testJwt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "BANKING_SEED_MOCK_DATA=false",
      "spring.datasource.url=jdbc:h2:mem:cards_seed_off;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
    })
class CardControllerSeedOffTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void allEndpointsReturnEmptyListsWhenNotSeeded() throws Exception {
    mockMvc
        .perform(get("/cards").with(testJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
    mockMvc
        .perform(get("/cards/authorizations").with(testJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
    mockMvc
        .perform(get("/cards/chargebacks").with(testJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }
}
