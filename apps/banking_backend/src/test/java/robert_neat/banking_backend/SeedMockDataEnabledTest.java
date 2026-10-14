package robert_neat.banking_backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import robert_neat.banking_backend.loan.LoanApplication;
import robert_neat.banking_backend.loan.LoanApplicationRepository;

// Sets the env-var name (not banking.seed-mock-data) to exercise the real placeholder binding.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
        "BANKING_SEED_MOCK_DATA=true",
        "spring.datasource.url=jdbc:h2:mem:seed_on;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
class SeedMockDataEnabledTest {

    static final Map<String, Integer> EXPECTED_COUNTS = new LinkedHashMap<>();

    static {
        EXPECTED_COUNTS.put("customer", 12);
        EXPECTED_COUNTS.put("employee", 9);
        EXPECTED_COUNTS.put("bank_account", 12);
        EXPECTED_COUNTS.put("loan_application", 13);
        EXPECTED_COUNTS.put("loan_stage_transition", 7);
        EXPECTED_COUNTS.put("installment", 24);
        EXPECTED_COUNTS.put("payment_card", 5);
        EXPECTED_COUNTS.put("card_authorization_ref", 4);
        EXPECTED_COUNTS.put("chargeback_ref", 1);
        EXPECTED_COUNTS.put("collateral_entry", 5);
        EXPECTED_COUNTS.put("ebanking_user", 5);
        EXPECTED_COUNTS.put("customer_case", 5);
        EXPECTED_COUNTS.put("service_disposition", 3);
        EXPECTED_COUNTS.put("contact_history_entry", 3);
        EXPECTED_COUNTS.put("collections_case", 6);
        EXPECTED_COUNTS.put("collections_action", 3);
        EXPECTED_COUNTS.put("enforcement_case", 4);
        EXPECTED_COUNTS.put("regulatory_report", 5);
        EXPECTED_COUNTS.put("report_submission", 2);
        EXPECTED_COUNTS.put("dashboard_kpi_snapshot", 1);
        EXPECTED_COUNTS.put("portfolio_trend_point", 9);
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private LoanApplicationRepository loanApplications;

    @Test
    void seedsEveryTable() {
        EXPECTED_COUNTS.forEach((table, expected) -> assertThat(
                jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class))
                .as(table)
                .isEqualTo(expected));
    }

    @Test
    void loanReferencesSeededCustomer() {
        String name = jdbc.queryForObject(
                "SELECT c.full_name FROM loan_application l JOIN customer c ON c.id = l.customer_id"
                        + " WHERE l.id = 'LN-80231'",
                String.class);
        assertThat(name).isEqualTo("Jan Wiśniewski");
    }

    @Test
    @Transactional
    void jpaMappingReadsSeededRows() {
        LoanApplication loan = loanApplications.findById("LN-80231").orElseThrow();
        assertThat(loan.getCustomer().getFullName()).isEqualTo("Jan Wiśniewski");
        assertThat(loan.getCreatedAt()).isNotNull();
        assertThat(loanApplications.findByCustomerId("CUST-006"))
                .extracting(LoanApplication::getId)
                .containsExactly("LN-80203");
    }
}
