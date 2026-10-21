package robert_neat.governance_mock.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA entity for the audit log. Keeps record-style accessors (id(), actor(), ...) alongside
 * JavaBean getters so existing callers (controllers, CaseSlaController, tests) keep compiling
 * unchanged; Jackson serializes via the JavaBean getters only.
 */
@Entity
@Table(name = "audit_event", schema = "governance")
public class AuditEvent {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "actor", length = 50, nullable = false)
    private String actor;

    @Column(name = "action", length = 100, nullable = false)
    private String action;

    @Column(name = "entity_type", length = 50, nullable = false)
    private String entityType;

    @Column(name = "entity_id", length = 50, nullable = false)
    private String entityId;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "details")
    private String details;

    protected AuditEvent() {
        // JPA
    }

    public AuditEvent(
            String id,
            String actor,
            String action,
            String entityType,
            String entityId,
            Instant timestamp,
            String details) {
        this.id = id;
        this.actor = actor;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.timestamp = timestamp;
        this.details = details;
    }

    // Record-style accessors, kept for existing call sites.
    public String id() {
        return id;
    }

    public String actor() {
        return actor;
    }

    public String action() {
        return action;
    }

    public String entityType() {
        return entityType;
    }

    public String entityId() {
        return entityId;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public String details() {
        return details;
    }

    // JavaBean getters, used by Jackson for JSON serialization.
    public String getId() {
        return id;
    }

    public String getActor() {
        return actor;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getDetails() {
        return details;
    }
}
