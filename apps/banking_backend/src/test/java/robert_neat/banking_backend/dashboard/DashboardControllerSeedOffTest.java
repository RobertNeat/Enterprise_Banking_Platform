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
        "BANKING_SEED_MOCK_DATA=false",
        "spring.datasource.url=jdbc:h2:mem:dashboard_seed_off;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
class DashboardControllerSeedOffTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsReturnEmptyWhenNotSeeded() throws Exception {
        mockMvc.perform(get("/dashboard/recent-cases")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/dashboard/regulatory-alerts")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/dashboard/portfolio-trend")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void latestKpiSnapshotReturns404WhenNotSeeded() throws Exception {
        mockMvc.perform(get("/dashboard/kpi-snapshot/latest")).andExpect(status().isNotFound());
    }
}
