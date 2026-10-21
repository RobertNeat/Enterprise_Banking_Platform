package robert_neat.banking_backend.ebanking;

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
      "BANKING_SEED_MOCK_DATA=true",
      "spring.datasource.url=jdbc:h2:mem:ebanking_seed_on;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
    })
class EbankingControllerSeedOnTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void listsUsersWithPermissionsSplitAndAllowedStatuses() throws Exception {
    mockMvc
        .perform(get("/ebanking/users").with(testJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(5))
        .andExpect(
            jsonPath("$[*].id")
                .value(
                    org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyOrNullString()))))
        .andExpect(
            jsonPath("$[*].customer")
                .value(
                    org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyOrNullString()))))
        .andExpect(jsonPath("$[0].permissions").isArray())
        .andExpect(
            jsonPath("$[*].accessStatus")
                .value(
                    org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.in(
                            new String[] {"active", "locked", "pendingReset"}))));
  }

  @Test
  void lastLoginIsFlattenedInWarsawLocalTimeNotUtc() throws Exception {
    // EB-1001 last_login seed value is '2026-10-03 14:22:00+02:00' — UTC would wrongly
    // show 12:22, which is the exact regression this pins down.
    mockMvc
        .perform(get("/ebanking/users").with(testJwt()))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$[?(@.id=='EB-1001')].lastLogin")
                .value(org.hamcrest.Matchers.contains("2026-10-03 14:22")));
  }
}
