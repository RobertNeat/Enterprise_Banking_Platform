package robert_neat.governance_mock.incident;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA entity for the ICT incident registry (DORA). Keeps record-style accessors (id(), source(),
 * ...) alongside JavaBean getters so existing callers (controllers, tests) keep compiling
 * unchanged; Jackson serializes via the JavaBean getters only.
 */
@Entity
@Table(name = "ict_incident", schema = "governance")
public class IctIncident {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "source", length = 100, nullable = false)
    private String source;

    @Column(name = "description", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 10, nullable = false)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    private IncidentStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected IctIncident() {
        // JPA
    }

    public IctIncident(
            String id,
            String source,
            String description,
            Severity severity,
            IncidentStatus status,
            Instant createdAt,
            Instant resolvedAt) {
        this.id = id;
        this.source = source;
        this.description = description;
        this.severity = severity;
        this.status = status;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
    }

    public IctIncident resolved(Instant resolvedAt) {
        return new IctIncident(id, source, description, severity, IncidentStatus.RESOLVED, createdAt, resolvedAt);
    }

    // Record-style accessors, kept for existing call sites.
    public String id() {
        return id;
    }

    public String source() {
        return source;
    }

    public String description() {
        return description;
    }

    public Severity severity() {
        return severity;
    }

    public IncidentStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant resolvedAt() {
        return resolvedAt;
    }

    // JavaBean getters, used by Jackson for JSON serialization.
    public String getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getDescription() {
        return description;
    }

    public Severity getSeverity() {
        return severity;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
