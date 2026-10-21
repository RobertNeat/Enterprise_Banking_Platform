package robert_neat.banking_backend.approvals;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Proxy to governance_mock's GET /audit-events — backs the "Historia" tab/dialog on loan, case,
 * card and enforcement detail views. governance_mock already records every audit event written by
 * recordAuditEvent(); this just exposes the read side.
 */
@RestController
@RequestMapping("/audit-events")
public class AuditEventsController {

    private final ApprovalsService approvalsService;

    public AuditEventsController(ApprovalsService approvalsService) {
        this.approvalsService = approvalsService;
    }

    @GetMapping
    public List<AuditEventDto> list(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId) {
        return approvalsService.listAuditEvents(entityType, entityId);
    }
}
