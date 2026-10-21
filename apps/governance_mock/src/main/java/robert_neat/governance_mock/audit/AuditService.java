package robert_neat.governance_mock.audit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public AuditEvent record(String actor, String action, String entityType, String entityId, String details) {
        if (isBlank(actor) || isBlank(action) || isBlank(entityType) || isBlank(entityId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "actor, action, entityType i entityId sa wymagane");
        }
        String id = UUID.randomUUID().toString();
        AuditEvent event = new AuditEvent(id, actor, action, entityType, entityId, Instant.now(), details);
        return auditEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> list(String entityType, String entityId) {
        // entityType is matched case-insensitively, entityId case-sensitively — same semantics as
        // the previous in-memory filter.
        if (entityType != null && entityId != null) {
            return auditEventRepository.findByEntityTypeIgnoreCaseAndEntityIdOrderByTimestampAsc(entityType, entityId);
        }
        if (entityType != null) {
            return auditEventRepository.findByEntityTypeIgnoreCaseOrderByTimestampAsc(entityType);
        }
        if (entityId != null) {
            return auditEventRepository.findByEntityIdOrderByTimestampAsc(entityId);
        }
        return auditEventRepository.findAllByOrderByTimestampAsc();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
