package robert_neat.governance_mock.approval;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = "GOVERNANCE_SEED_MOCK_DATA=false")
class ApprovalSeedingDisabledTest {

    @Autowired
    private ApprovalService approvalService;

    @Test
    void seedsNothingWhenDisabled() {
        assertThat(approvalService.list(null)).isEmpty();
    }
}
