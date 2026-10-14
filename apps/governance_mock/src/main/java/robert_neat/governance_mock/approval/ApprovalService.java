package robert_neat.governance_mock.approval;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import robert_neat.governance_mock.audit.AuditService;

import jakarta.annotation.PostConstruct;
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
    private final boolean seedMockData;

    public ApprovalService(
            AuditService auditService,
            @Value("${governance.seed-mock-data:true}") boolean seedMockData) {
        this.auditService = auditService;
        this.seedMockData = seedMockData;
    }

    /**
     * Seeds approvals for loan applications from banking_backend's loan_application /
     * loan_stage_transition seed (db/changelog/seed/002-loan.sql) so the maker-checker demo has
     * data consistent with the backend's state, rather than starting from an empty queue.
     * Fixed ids (not random) so docs/tests can reference them directly.
     *
     * LN-80231 already transitioned decision -> contract in the backend seed (approved by
     * PRC-10234, the loan advisor who also requested/closed the loan) — seeded here already
     * APPROVED, decided by PRC-00001 (admin, a different employee than the requester, satisfying
     * governance_mock's own four-eyes check) to keep an audit trail consistent with "decided by
     * someone other than the maker".
     *
     * LN-80255 is still sitting in stage='decision' in the backend seed with NO transition row
     * yet (nobody has decided it) — seeded here as PENDING, requested by PRC-20456 (the credit
     * risk analyst who scored it), so the approvals queue demo has something left to act on.
     */
    @PostConstruct
    void seed() {
        if (!seedMockData) {
            return;
        }
        seedDecided("11111111-1111-4111-8111-111111111001", "LOAN_STAGE_TRANSITION", "PRC-10234",
                "LN-80231: affordability -> decision -> contract (480000.00 PLN, retail mortgage)",
                "PRC-00001", Instant.parse("2026-09-02T07:45:00Z"));
        seedPending("22222222-2222-4222-8222-222222222002", "LOAN_STAGE_TRANSITION", "PRC-20456",
                "LN-80255: scoring -> decision (2200000.00 PLN, business investment, score 655)");
    }

    private void seedPending(String id, String type, String requestedBy, String payload) {
        approvals.put(id, new ApprovalRequestRecord(
                id, type, requestedBy, payload, ApprovalStatus.PENDING, Instant.now(), null, null));
    }

    private void seedDecided(String id, String type, String requestedBy, String payload,
            String decidedBy, Instant decidedAt) {
        approvals.put(id, new ApprovalRequestRecord(
                id, type, requestedBy, payload, ApprovalStatus.APPROVED, decidedAt.minusSeconds(3600),
                decidedBy, decidedAt));
        auditService.record(decidedBy, "APPROVAL_APPROVED", "APPROVAL", id,
                "type=" + type + "; requestedBy=" + requestedBy + "; payload=" + payload);
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
