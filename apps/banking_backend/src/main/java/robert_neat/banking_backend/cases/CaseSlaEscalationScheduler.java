package robert_neat.banking_backend.cases;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * F8 — periodic sweep that auto-escalates cases whose SLA has lapsed while nobody manually
 * escalated them (sla_due_at &lt; now, status still 'new'/'inProgress'). Reuses
 * {@link CaseActionsService#autoEscalateIfSlaExpired(String)}, which re-confirms the SLA breach
 * against governance_mock and only then applies escalationSource='AUTO_SLA' — the status filter
 * below is the only guard against duplicate AUTO_ESCALATED audit events, since governance_mock
 * does not deduplicate (see CaseActionsService javadoc on resolve()). Disabled by default in the
 * test classpath (src/test/resources/application.properties) so @SpringBootTest contexts that
 * mock GovernanceClient, or seed cases with a past sla_due_at, don't have the sweep race their
 * assertions.
 */
@Component
@ConditionalOnProperty(name = "banking.cases.sla-escalation.enabled", havingValue = "true", matchIfMissing = true)
public class CaseSlaEscalationScheduler {

    private static final Logger log = LoggerFactory.getLogger(CaseSlaEscalationScheduler.class);
    private static final List<String> OPEN_STATUSES = List.of("new", "inProgress");

    private final CustomerCaseRepository customerCases;
    private final CaseActionsService caseActionsService;

    public CaseSlaEscalationScheduler(CustomerCaseRepository customerCases, CaseActionsService caseActionsService) {
        this.customerCases = customerCases;
        this.caseActionsService = caseActionsService;
    }

    @Scheduled(
            initialDelayString = "${banking.cases.sla-escalation.interval:PT5M}",
            fixedDelayString = "${banking.cases.sla-escalation.interval:PT5M}")
    public void escalateOverdueCases() {
        List<CustomerCase> overdue = customerCases.findBySlaDueAtBeforeAndStatusIn(Instant.now(), OPEN_STATUSES);
        for (CustomerCase customerCase : overdue) {
            String caseId = customerCase.getId();
            try {
                caseActionsService.autoEscalateIfSlaExpired(caseId);
            } catch (RuntimeException ex) {
                // One case failing (e.g. governance_mock unavailable, or a manual escalate/resolve
                // racing this sweep) must not abort the rest of the batch.
                log.warn("AUTO_SLA escalation sweep failed for case {}: {}", caseId, ex.getMessage());
            }
        }
    }
}
