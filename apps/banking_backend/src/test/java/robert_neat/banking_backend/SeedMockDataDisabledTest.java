package robert_neat.banking_backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
        "BANKING_SEED_MOCK_DATA=false",
        "spring.datasource.url=jdbc:h2:mem:seed_off;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
class SeedMockDataDisabledTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void schemaAppliedButAllTablesEmpty() {
        SeedMockDataEnabledTest.EXPECTED_COUNTS.keySet().forEach(table -> assertThat(
                jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class))
                .as(table)
                .isZero());
    }
}
