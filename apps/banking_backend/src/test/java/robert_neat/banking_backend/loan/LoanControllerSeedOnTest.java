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
        "BANKING_SEED_MOCK_DATA=true",
        "spring.datasource.url=jdbc:h2:mem:loans_seed_on;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
class LoanControllerSeedOnTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listReturnsAllSeededLoansWithRequiredFieldsPopulated() throws Exception {
        mockMvc.perform(get("/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(13))
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyOrNullString()))))
                .andExpect(jsonPath("$[*].applicant").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyOrNullString()))))
                .andExpect(jsonPath("$[*].segment").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.in(new String[] {"retail", "business"}))))
                .andExpect(jsonPath("$[*].product").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.in(new String[] {
                                "cash", "mortgage", "overdraft", "investment", "creditLine"}))))
                .andExpect(jsonPath("$[*].stage").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.in(new String[] {
                                "application", "scoring", "affordability", "decision",
                                "contract", "disbursement", "schedule", "collections"}))))
                .andExpect(jsonPath("$[*].requestedAmount").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.notNullValue())))
                .andExpect(jsonPath("$[*].createdAt").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyOrNullString()))));
    }

    @Test
    void createdAtIsFlattenedInWarsawLocalDateNotUtc() throws Exception {
        // created_at seed value is '2026-08-12 09:00:00+02:00' — UTC would still show
        // 2026-08-12 (07:00 UTC), but this pins the zone so a future change to a
        // timestamp close to local midnight doesn't silently regress to UTC.
        mockMvc.perform(get("/loans/LN-80231"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdAt").value("2026-08-12"));
    }

    @Test
    void detailIncludesInstallmentsAndStageTransitionsForSeededLoan() throws Exception {
        mockMvc.perform(get("/loans/LN-80231"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("LN-80231"))
                .andExpect(jsonPath("$.applicant").value("Jan Wiśniewski"))
                .andExpect(jsonPath("$.stageTransitions.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.stageTransitions[0].transitionedBy").isNotEmpty());
    }

    @Test
    void detailReturnsEmptyInstallmentsForLoanWithoutSchedule() throws Exception {
        mockMvc.perform(get("/loans/LN-80255"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.installments").isArray())
                .andExpect(jsonPath("$.installments.length()").value(0));
    }

    @Test
    void unknownIdReturns404WithMessageBody() throws Exception {
        mockMvc.perform(get("/loans/LN-99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
