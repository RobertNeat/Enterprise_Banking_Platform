package robert_neat.governance_mock.incident;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IctIncidentServiceTest {

    private IctIncidentService incidentService;

    @BeforeEach
    void setUp() {
        incidentService = new IctIncidentService();
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
