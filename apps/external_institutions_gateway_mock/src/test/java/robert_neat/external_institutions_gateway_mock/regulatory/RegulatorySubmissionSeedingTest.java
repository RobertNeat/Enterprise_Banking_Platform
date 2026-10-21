package robert_neat.external_institutions_gateway_mock.regulatory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(
        properties = {
            "GATEWAY_SEED_MOCK_DATA=true",
            "gateway.submission.delay-ms=50",
            "spring.datasource.url=jdbc:h2:mem:gateway_regulatory_seeding_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
        })
class RegulatorySubmissionSeedingTest {

    @Autowired
    private RegulatoryGatewayService regulatoryGatewayService;

    @Test
    void seededSubmissionsMatchBankingBackend() {
        RegulatorySubmission fatca = regulatoryGatewayService.get("c19e8d42-0a7b-4f3e-8b65-91d4e2f7a308");
        assertThat(fatca.status()).isEqualTo(SubmissionStatus.ACCEPTED);
        assertThat(fatca.reportId()).isEqualTo("RPT-FATCA-009");

        RegulatorySubmission nbp = pollUntilResolved("7f3c2a91-4b6e-4d2a-9c1f-2e8b5d6a0f14");
        // Deterministic rule: "RPT-NBP-014".hashCode() mod 10 != 0 -> ACCEPTED.
        assertThat(nbp.status()).isEqualTo(SubmissionStatus.ACCEPTED);
        assertThat(nbp.reportId()).isEqualTo("RPT-NBP-014");
    }

    private RegulatorySubmission pollUntilResolved(String submissionId) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (Instant.now().isBefore(deadline)) {
            RegulatorySubmission submission = regulatoryGatewayService.get(submissionId);
            if (submission.status() != SubmissionStatus.PENDING) {
                return submission;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        throw new AssertionError("Submission did not resolve in time: " + submissionId);
    }
}
