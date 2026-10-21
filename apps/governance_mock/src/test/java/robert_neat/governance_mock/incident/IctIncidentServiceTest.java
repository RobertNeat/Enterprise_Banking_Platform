package robert_neat.governance_mock.incident;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Promoted from a plain unit test (new IctIncidentService()) to @SpringBootTest, since
 * IctIncidentService now needs a JPA repository/EntityManager. The H2 database is shared for the
 * whole surefire JVM run (DB_CLOSE_DELAY=-1), so this test gets its own isolated schema/url and
 * clears the table in @BeforeEach rather than relying on a fresh in-memory map per test.
 */
@SpringBootTest
@TestPropertySource(
        properties =
                "spring.datasource.url=jdbc:h2:mem:ict_incident_service_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE")
class IctIncidentServiceTest {

    @Autowired
    private IctIncidentService incidentService;

    @Autowired
    private IctIncidentRepository incidentRepository;

    @BeforeEach
    void setUp() {
        incidentRepository.deleteAll();
        incidentService.create("CARD_PROCESSOR", "card outage", Severity.HIGH);
        incidentService.create("PAYMENT_GATEWAY", "payment delay", Severity.LOW);
        String resolvedId = incidentService.create("IDENTITY_ACCESS", "login degraded", Severity.MEDIUM).id();
        incidentService.resolve(resolvedId);
    }

    @Test
    void listWithoutFiltersReturnsAllIncidents() {
        assertThat(incidentService.list(null, null)).hasSize(3);
    }

    @Test
    void listFiltersByStatus() {
        List<IctIncident> open = incidentService.list(IncidentStatus.OPEN, null);
        assertThat(open).hasSize(2);
        assertThat(open).allMatch(i -> i.status() == IncidentStatus.OPEN);

        List<IctIncident> resolved = incidentService.list(IncidentStatus.RESOLVED, null);
        assertThat(resolved).hasSize(1);
        assertThat(resolved.get(0).status()).isEqualTo(IncidentStatus.RESOLVED);
    }

    @Test
    void listFiltersBySeverity() {
        List<IctIncident> high = incidentService.list(null, Severity.HIGH);
        assertThat(high).hasSize(1);
        assertThat(high.get(0).severity()).isEqualTo(Severity.HIGH);

        List<IctIncident> low = incidentService.list(null, Severity.LOW);
        assertThat(low).hasSize(1);
        assertThat(low.get(0).severity()).isEqualTo(Severity.LOW);
    }

    @Test
    void listFiltersByStatusAndSeverityCombined() {
        List<IctIncident> result = incidentService.list(IncidentStatus.OPEN, Severity.HIGH);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).source()).isEqualTo("CARD_PROCESSOR");

        List<IctIncident> noMatch = incidentService.list(IncidentStatus.OPEN, Severity.MEDIUM);
        assertThat(noMatch).isEmpty();
    }
}
