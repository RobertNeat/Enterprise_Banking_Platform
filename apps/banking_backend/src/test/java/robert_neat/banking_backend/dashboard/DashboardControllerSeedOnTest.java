package robert_neat.banking_backend.dashboard;

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
        "spring.datasource.url=jdbc:h2:mem:dashboard_seed_on;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
class DashboardControllerSeedOnTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void recentCasesReturnsTop4ByRegisteredAtDesc() throws Exception {
        mockMvc.perform(get("/dashboard/recent-cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].customer").isNotEmpty())
                .andExpect(jsonPath("$[0].owner").isNotEmpty());
    }

    @Test
    void regulatoryAlertsExcludeAcceptedAndReturnTop3() throws Exception {
        mockMvc.perform(get("/dashboard/regulatory-alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.status=='accepted')]").isEmpty());
    }

    @Test
    void latestKpiSnapshotReturnsSeededRow() throws Exception {
        mockMvc.perform(get("/dashboard/kpi-snapshot/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snapshotDate").isNotEmpty())
                .andExpect(jsonPath("$.activeLoansCount").isNumber());
    }

    @Test
    void portfolioTrendReturnsAllSeededPoints() throws Exception {
        mockMvc.perform(get("/dashboard/portfolio-trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9));
    }
}
