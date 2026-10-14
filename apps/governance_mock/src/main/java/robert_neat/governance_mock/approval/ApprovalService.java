package robert_neat.governance_mock.approval;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import robert_neat.governance_mock.audit.AuditService;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ApprovalService {

    private final Map<String, ApprovalRequestRecord> approvals = new ConcurrentHashMap<>();
    private final AuditService auditService;

    public ApprovalService(AuditService auditService) {
        this.auditService = auditService;
    }

    public ApprovalRequestRecord create(String type, String requestedBy, String payload) {
        if (isBlank(type) || isBlank(requestedBy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type i requestedBy sa wymagane");
        }
        String id = UUID.randomUUID().toString();
        ApprovalRequestRecord request = new ApprovalRequestRecord(
                id, type, requestedBy, payload, ApprovalStatus.PENDING, Instant.now(), null, null);
        approvals.put(id, request);
        return request;
    }

    public ApprovalRequestRecord get(String id) {
        ApprovalRequestRecord request = approvals.get(id);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval request nie znaleziony: " + id);
        }
        return request;
    }

    public List<ApprovalRequestRecord> list(ApprovalStatus status) {
        return approvals.values().stream()
                .filter(a -> status == null || status == a.status())
                .sorted(Comparator.comparing(ApprovalRequestRecord::createdAt))
                .toList();
    }

    /**
     * Requires decidedBy != requestedBy (409, zasada czworu oczu) and status == PENDING (409).
     * Audit entry is written only after the state change succeeds.
     */
    public ApprovalRequestRecord decide(String id, Decision decision, String decidedBy) {
        if (decision == null || isBlank(decidedBy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "decision i decidedBy sa wymagane");
        }

        AtomicReference<ApprovalRequestRecord> updated = new AtomicReference<>();
        approvals.compute(id, (key, existing) -> {
            if (existing == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval request nie znaleziony: " + id);
            }
            if (existing.status() != ApprovalStatus.PENDING) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Approval request juz zdecydowany: " + existing.status());
            }
            if (decidedBy.trim().equalsIgnoreCase(existing.requestedBy().trim())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "decidedBy musi byc inny niz requestedBy (zasada czworu oczu)");
            }
            ApprovalStatus newStatus = decision == Decision.APPROVED ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED;
            ApprovalRequestRecord result = existing.withDecision(newStatus, decidedBy, Instant.now());
            updated.set(result);
            return result;
        });

        ApprovalRequestRecord result = updated.get();
        String action = result.status() == ApprovalStatus.APPROVED ? "APPROVAL_APPROVED" : "APPROVAL_REJECTED";
        auditService.record(decidedBy, action, "APPROVAL", id,
                "type=" + result.type() + "; requestedBy=" + result.requestedBy() + "; payload=" + result.payload());
        return result;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
