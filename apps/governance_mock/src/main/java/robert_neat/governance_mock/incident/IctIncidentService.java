package robert_neat.governance_mock.incident;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** ICT incident registry (DORA), persisted via JPA. No audit coupling per spec — only approvals/SLA auto-audit. */
@Service
public class IctIncidentService {

    private final IctIncidentRepository incidentRepository;

    public IctIncidentService(IctIncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @Transactional
    public IctIncident create(String source, String description, Severity severity) {
        if (isBlank(source) || isBlank(description) || severity == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "source, description i severity sa wymagane");
        }
        String id = UUID.randomUUID().toString();
        IctIncident incident = new IctIncident(id, source, description, severity, IncidentStatus.OPEN, Instant.now(), null);
        return incidentRepository.save(incident);
    }

    /**
     * status/severity are optional filters; null means no filter on that field. Unrecognized values
     * are rejected before reaching this method - the controller binds them directly as enum
     * parameters (same mechanism as ApprovalService.list/ApprovalController#list), so Spring MVC's
     * default enum conversion throws and the module's default error handling turns that into a 400.
     */
    @Transactional(readOnly = true)
    public List<IctIncident> list(IncidentStatus status, Severity severity) {
        if (status != null && severity != null) {
            return incidentRepository.findByStatusAndSeverityOrderByCreatedAtAsc(status, severity);
        }
        if (status != null) {
            return incidentRepository.findByStatusOrderByCreatedAtAsc(status);
        }
        if (severity != null) {
            return incidentRepository.findBySeverityOrderByCreatedAtAsc(severity);
        }
        return incidentRepository.findAllByOrderByCreatedAtAsc();
    }

    @Transactional
    public IctIncident resolve(String id) {
        IctIncident existing = incidentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incydent ICT nie znaleziony: " + id));
        if (existing.status() == IncidentStatus.RESOLVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Incydent ICT juz rozwiazany: " + id);
        }
        IctIncident resolved = existing.resolved(Instant.now());
        return incidentRepository.save(resolved);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
