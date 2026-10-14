package robert_neat.governance_mock.audit;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/audit-events")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping
    public ResponseEntity<AuditEvent> create(@RequestBody AuditEventRequest request) {
        AuditEvent event = auditService.record(
                request.actor(), request.action(), request.entityType(), request.entityId(), request.details());
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    @GetMapping
    public List<AuditEvent> list(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId) {
        return auditService.list(entityType, entityId);
    }
}
