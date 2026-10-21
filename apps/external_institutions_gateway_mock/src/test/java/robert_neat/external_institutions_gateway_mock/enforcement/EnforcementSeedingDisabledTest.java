package robert_neat.external_institutions_gateway_mock.enforcement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(
        properties = {
            "GATEWAY_SEED_MOCK_DATA=false",
            "spring.datasource.url=jdbc:h2:mem:gateway_enforcement_seeding_disabled_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
        })
class EnforcementSeedingDisabledTest {

    @Autowired
    private EnforcementService enforcementService;

    @Test
    void seedsNothingWhenDisabled() {
        assertThat(enforcementService.list()).isEmpty();
    }
}
