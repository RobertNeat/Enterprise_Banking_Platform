package robert_neat.governance_mock.incident;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/** In-memory ICT incident registry (DORA). No audit coupling per spec — only approvals/SLA auto-audit. */
@Service
public class IctIncidentService {

    private final Map<String, IctIncident> incidents = new ConcurrentHashMap<>();

    public IctIncident create(String source, String description, Severity severity) {
        if (isBlank(source) || isBlank(description) || severity == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "source, description i severity sa wymagane");
        }
        String id = UUID.randomUUID().toString();
        IctIncident incident = new IctIncident(id, source, description, severity, IncidentStatus.OPEN, Instant.now(), null);
        incidents.put(id, incident);
        return incident;
    }

    /**
     * status/severity are optional filters; null means no filter on that field. Unrecognized values
     * are rejected before reaching this method - the controller binds them directly as enum
     * parameters (same mechanism as ApprovalService.list/ApprovalController#list), so Spring MVC's
     * default enum conversion throws and the module's default error handling turns that into a 400.
     */
    public List<IctIncident> list(IncidentStatus status, Severity severity) {
        return incidents.values().stream()
                .filter(i -> status == null || i.status() == status)
                .filter(i -> severity == null || i.severity() == severity)
                .sorted(Comparator.comparing(IctIncident::createdAt))
                .toList();
    }

    public IctIncident resolve(String id) {
        AtomicReference<IctIncident> updated = new AtomicReference<>();
        incidents.compute(id, (key, existing) -> {
            if (existing == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Incydent ICT nie znaleziony: " + id);
            }
            if (existing.status() == IncidentStatus.RESOLVED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Incydent ICT juz rozwiazany: " + id);
            }
            IctIncident result = existing.resolved(Instant.now());
            updated.set(result);
            return result;
        });
        return updated.get();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
