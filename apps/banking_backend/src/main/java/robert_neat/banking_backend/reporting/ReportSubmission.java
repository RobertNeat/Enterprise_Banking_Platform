package robert_neat.banking_backend.reporting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "report_submission")
public class ReportSubmission {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "regulatory_report_id", nullable = false)
  private RegulatoryReport regulatoryReport;

  @Column(name = "gateway_submission_id", length = 36, nullable = false)
  private String gatewaySubmissionId;

  @Column(name = "gateway_status", length = 15, nullable = false)
  private String gatewayStatus;

  @Column(name = "validation_errors", length = 1000)
  private String validationErrors;

  @Column(name = "submitted_at", nullable = false)
  private Instant submittedAt;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public RegulatoryReport getRegulatoryReport() {
    return regulatoryReport;
  }

  public void setRegulatoryReport(RegulatoryReport regulatoryReport) {
    this.regulatoryReport = regulatoryReport;
  }

  public String getGatewaySubmissionId() {
    return gatewaySubmissionId;
  }

  public void setGatewaySubmissionId(String gatewaySubmissionId) {
    this.gatewaySubmissionId = gatewaySubmissionId;
  }

  public String getGatewayStatus() {
    return gatewayStatus;
  }

  public void setGatewayStatus(String gatewayStatus) {
    this.gatewayStatus = gatewayStatus;
  }

  public String getValidationErrors() {
    return validationErrors;
  }

  public void setValidationErrors(String validationErrors) {
    this.validationErrors = validationErrors;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public void setSubmittedAt(Instant submittedAt) {
    this.submittedAt = submittedAt;
  }

  public Instant getResolvedAt() {
    return resolvedAt;
  }

  public void setResolvedAt(Instant resolvedAt) {
    this.resolvedAt = resolvedAt;
  }
}
