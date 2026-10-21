package robert_neat.governance_mock.approval;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(
        properties = {
            "GOVERNANCE_SEED_MOCK_DATA=false",
            "spring.datasource.url=jdbc:h2:mem:approval_seeding_off;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
        })
class ApprovalSeedingDisabledTest {

    @Autowired
    private ApprovalService approvalService;

    @Test
    void seedsNothingWhenDisabled() {
        assertThat(approvalService.list(null)).isEmpty();
    }
}
