package robert_neat.governance_mock.audit;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuditService {

    private final Map<String, AuditEvent> events = new ConcurrentHashMap<>();

    public AuditEvent record(String actor, String action, String entityType, String entityId, String details) {
        if (isBlank(actor) || isBlank(action) || isBlank(entityType) || isBlank(entityId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "actor, action, entityType i entityId sa wymagane");
        }
        String id = UUID.randomUUID().toString();
        AuditEvent event = new AuditEvent(id, actor, action, entityType, entityId, Instant.now(), details);
        events.put(id, event);
        return event;
    }

    public List<AuditEvent> list(String entityType, String entityId) {
        return events.values().stream()
                .filter(e -> entityType == null || entityType.equalsIgnoreCase(e.entityType()))
                .filter(e -> entityId == null || entityId.equals(e.entityId()))
                .sorted(Comparator.comparing(AuditEvent::timestamp))
                .toList();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
