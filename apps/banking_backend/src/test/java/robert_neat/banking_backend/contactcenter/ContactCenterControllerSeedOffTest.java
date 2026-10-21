package robert_neat.banking_backend.contactcenter;

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
      "spring.datasource.url=jdbc:h2:mem:contactcenter_seed_off;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
    })
class ContactCenterControllerSeedOffTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void listsReturnEmptyWhenNotSeeded() throws Exception {
    mockMvc
        .perform(get("/contact-center/dispositions").with(testJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
    mockMvc
        .perform(get("/contact-center/history").with(testJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void profileReturns404WhenNotSeeded() throws Exception {
    mockMvc
        .perform(get("/contact-center/profile/CUST-001").with(testJwt()))
        .andExpect(status().isNotFound());
  }
}
