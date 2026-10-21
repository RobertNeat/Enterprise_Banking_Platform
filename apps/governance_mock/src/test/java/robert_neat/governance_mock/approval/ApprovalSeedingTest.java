package robert_neat.governance_mock.approval;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(
        properties = {
            "GOVERNANCE_SEED_MOCK_DATA=true",
            "spring.datasource.url=jdbc:h2:mem:approval_seeding_on;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
        })
class ApprovalSeedingTest {

    @Autowired
    private ApprovalService approvalService;

    @Test
    void seedsLoanStageTransitionApprovalsMatchingBankingBackend() {
        List<ApprovalRequestRecord> all = approvalService.list(null);
        assertThat(all).extracting(ApprovalRequestRecord::id).contains(
                "11111111-1111-4111-8111-111111111001", "22222222-2222-4222-8222-222222222002");

        ApprovalRequestRecord approved = approvalService.get("11111111-1111-4111-8111-111111111001");
        assertThat(approved.status()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(approved.requestedBy()).isEqualTo("PRC-10234");
        assertThat(approved.decidedBy()).isEqualTo("PRC-00001");

        ApprovalRequestRecord pending = approvalService.get("22222222-2222-4222-8222-222222222002");
        assertThat(pending.status()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(pending.requestedBy()).isEqualTo("PRC-20456");
    }
}
