package robert_neat.banking_backend.collections;

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
      "spring.datasource.url=jdbc:h2:mem:collections_seed_off;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
    })
class CollectionsControllerSeedOffTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void returnsEmptyListWhenNotSeeded() throws Exception {
    mockMvc
        .perform(get("/collections").with(testJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void actionsReturn404WhenCaseUnknown() throws Exception {
    mockMvc
        .perform(get("/collections/DBT-6601/actions").with(testJwt()))
        .andExpect(status().isNotFound());
  }
}
