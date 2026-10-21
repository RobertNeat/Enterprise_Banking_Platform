package robert_neat.external_institutions_gateway_mock.enforcement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(
        properties = {
            "GATEWAY_SEED_MOCK_DATA=true",
            "spring.datasource.url=jdbc:h2:mem:gateway_enforcement_seeding_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
        })
class EnforcementSeedingTest {

    @Autowired
    private EnforcementService enforcementService;

    @Test
    void seedsEnforcementCasesMatchingBankingBackend() {
        List<EnforcementCase> all = enforcementService.list();
        assertThat(all).extracting(EnforcementCase::caseNumber)
                .contains("KM 1123/26", "KM 998/26", "KM 887/25", "KM 1145/26");

        EnforcementCase active = all.stream()
                .filter(c -> c.caseNumber().equals("KM 1123/26")).findFirst().orElseThrow();
        assertThat(active.status()).isEqualTo(EnforcementStatus.ACTIVE);
        assertThat(active.customer()).isEqualTo("CUST-006");

        EnforcementCase closed = all.stream()
                .filter(c -> c.caseNumber().equals("KM 887/25")).findFirst().orElseThrow();
        assertThat(closed.status()).isEqualTo(EnforcementStatus.CLOSED);
    }
}
