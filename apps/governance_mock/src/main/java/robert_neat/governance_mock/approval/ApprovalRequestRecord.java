package robert_neat.governance_mock.approval;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA entity for the maker-checker approval queue. Keeps record-style accessors (id(), type(),
 * ...) alongside JavaBean getters so existing callers (controllers, tests, CaseSlaController-style
 * usages elsewhere) keep compiling unchanged; Jackson serializes via the JavaBean getters only.
 */
@Entity
@Table(name = "approval_request", schema = "governance")
public class ApprovalRequestRecord {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "type", length = 100, nullable = false)
    private String type;

    @Column(name = "requested_by", length = 50, nullable = false)
    private String requestedBy;

    @Column(name = "payload")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    private ApprovalStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "decided_by", length = 50)
    private String decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    protected ApprovalRequestRecord() {
        // JPA
    }

    public ApprovalRequestRecord(
            String id,
            String type,
            String requestedBy,
            String payload,
            ApprovalStatus status,
            Instant createdAt,
            String decidedBy,
            Instant decidedAt) {
        this.id = id;
        this.type = type;
        this.requestedBy = requestedBy;
        this.payload = payload;
        this.status = status;
        this.createdAt = createdAt;
        this.decidedBy = decidedBy;
        this.decidedAt = decidedAt;
    }

    public ApprovalRequestRecord withDecision(ApprovalStatus newStatus, String decidedBy, Instant decidedAt) {
        return new ApprovalRequestRecord(id, type, requestedBy, payload, newStatus, createdAt, decidedBy, decidedAt);
    }

    // Record-style accessors, kept for existing call sites.
    public String id() {
        return id;
    }

    public String type() {
        return type;
    }

    public String requestedBy() {
        return requestedBy;
    }

    public String payload() {
        return payload;
    }

    public ApprovalStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public String decidedBy() {
        return decidedBy;
    }

    public Instant decidedAt() {
        return decidedAt;
    }

    // JavaBean getters, used by Jackson for JSON serialization.
    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public String getPayload() {
        return payload;
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
