package robert_neat.external_institutions_gateway_mock.regulatory;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for a submission to the KNF/NBP/BFG reporting gateway. {@code validationErrors} is a
 * {@code List<String>} on the old in-memory record — the most JPA-idiomatic mapping for that is
 * {@code @ElementCollection} with its own table, rather than serializing to a delimited/JSON
 * column, so that is what is used here (one row per error message, in
 * {@code gateway.regulatory_submission_error}). Fetched EAGER: open-in-view is false, and the
 * controller/@Transactional service boundary returns this entity straight to Jackson, so a LAZY
 * collection would throw LazyInitializationException on serialization.
 */
@Entity
@Table(name = "regulatory_submission", schema = "gateway")
public class RegulatorySubmission {

    @Id
    @Column(name = "submission_id", length = 36)
    private String submissionId;

    @Column(name = "report_id", length = 50, nullable = false)
    private String reportId;

    @Column(name = "recipient", length = 20, nullable = false)
    private String recipient;

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Column(name = "period", length = 20, nullable = false)
    private String period;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private SubmissionStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "regulatory_submission_error",
            schema = "gateway",
            joinColumns = @JoinColumn(name = "submission_id"))
    @OrderColumn(name = "error_order")
    @Column(name = "error_message", nullable = false)
    private List<String> validationErrors = new ArrayList<>();

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected RegulatorySubmission() {
        // JPA
    }

    public RegulatorySubmission(
            String submissionId,
            String reportId,
            String recipient,
            String name,
            String period,
            SubmissionStatus status,
            List<String> validationErrors,
            Instant submittedAt,
            Instant resolvedAt) {
        this.submissionId = submissionId;
        this.reportId = reportId;
        this.recipient = recipient;
        this.name = name;
        this.period = period;
        this.status = status;
        this.validationErrors = new ArrayList<>(validationErrors);
        this.submittedAt = submittedAt;
        this.resolvedAt = resolvedAt;
    }

    public RegulatorySubmission withResolution(SubmissionStatus status, List<String> validationErrors, Instant resolvedAt) {
        return new RegulatorySubmission(submissionId, reportId, recipient, name, period,
                status, validationErrors, submittedAt, resolvedAt);
    }

    // Record-style accessors, kept for existing call sites (tests, UI templates).
    public String submissionId() {
        return submissionId;
    }

    public String reportId() {
        return reportId;
    }

    public String recipient() {
        return recipient;
    }

    public String name() {
        return name;
    }

    public String period() {
        return period;
    }

    public SubmissionStatus status() {
        return status;
    }

    public List<String> validationErrors() {
        return validationErrors;
    }

    public Instant submittedAt() {
        return submittedAt;
    }

    public Instant resolvedAt() {
        return resolvedAt;
    }

    // JavaBean getters, used by Jackson for JSON serialization.
    public String getSubmissionId() {
        return submissionId;
    }

    public String getReportId() {
        return reportId;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getName() {
        return name;
    }

    public String getPeriod() {
        return period;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
