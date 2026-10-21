package robert_neat.governance_mock.approval;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import robert_neat.governance_mock.audit.AuditService;

@Service
public class ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final AuditService auditService;

    public ApprovalService(ApprovalRepository approvalRepository, AuditService auditService) {
        this.approvalRepository = approvalRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ApprovalRequestRecord create(String type, String requestedBy, String payload) {
        if (isBlank(type) || isBlank(requestedBy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type i requestedBy sa wymagane");
        }
        String id = UUID.randomUUID().toString();
        ApprovalRequestRecord request = new ApprovalRequestRecord(
                id, type, requestedBy, payload, ApprovalStatus.PENDING, Instant.now(), null, null);
        return approvalRepository.save(request);
    }

    @Transactional(readOnly = true)
    public ApprovalRequestRecord get(String id) {
        return approvalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval request nie znaleziony: " + id));
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequestRecord> list(ApprovalStatus status) {
        if (status != null) {
            return approvalRepository.findByStatusOrderByCreatedAtAsc(status);
        }
        return approvalRepository.findAllByOrderByCreatedAtAsc();
    }

    /**
     * Requires decidedBy != requestedBy (409, zasada czworu oczu) and status == PENDING (409).
     * Audit entry is written only after the state change succeeds. Locks the row for the
     * duration of the transaction so two concurrent decide() calls can't both pass the PENDING
     * check and both write an audit event.
     */
    @Transactional
    public ApprovalRequestRecord decide(String id, Decision decision, String decidedBy) {
        if (decision == null || isBlank(decidedBy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "decision i decidedBy sa wymagane");
        }

        ApprovalRequestRecord existing = approvalRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval request nie znaleziony: " + id));
        if (existing.status() != ApprovalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Approval request juz zdecydowany: " + existing.status());
        }
        if (decidedBy.trim().equalsIgnoreCase(existing.requestedBy().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "decidedBy musi byc inny niz requestedBy (zasada czworu oczu)");
        }
        ApprovalStatus newStatus = decision == Decision.APPROVED ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED;
        ApprovalRequestRecord result = approvalRepository.save(existing.withDecision(newStatus, decidedBy, Instant.now()));

        String action = result.status() == ApprovalStatus.APPROVED ? "APPROVAL_APPROVED" : "APPROVAL_REJECTED";
        auditService.record(decidedBy, action, "APPROVAL", id,
                "type=" + result.type() + "; requestedBy=" + result.requestedBy() + "; payload=" + result.payload());
        return result;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
