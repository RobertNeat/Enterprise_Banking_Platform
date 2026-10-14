package robert_neat.governance_mock.casesla;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import robert_neat.governance_mock.audit.AuditEvent;
import robert_neat.governance_mock.audit.AuditService;

/**
 * F8: SLA rule evaluation for customer cases. banking_backend owns the case entity;
 * this service only judges the rule and logs the escalation — no case state is kept here.
 */
@RestController
@RequestMapping("/cases")
public class CaseSlaController {

    private final AuditService auditService;

    public CaseSlaController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping("/{caseId}/sla-check")
    public SlaCheckResponse check(@PathVariable String caseId, @RequestBody SlaCheckRequest request) {
        if (request == null || request.slaRemainingHours() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "slaRemainingHours jest wymagane");
        }

        boolean escalated = request.slaRemainingHours() <= 0;
        String auditEventId = null;
        if (escalated) {
            AuditEvent event = auditService.record(
                    "SYSTEM", "AUTO_ESCALATED", "CUSTOMER_CASE", caseId,
                    "slaRemainingHours=" + request.slaRemainingHours());
            auditEventId = event.id();
        }
        return new SlaCheckResponse(caseId, escalated, auditEventId);
    }
}
