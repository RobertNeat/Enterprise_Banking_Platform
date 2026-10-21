package robert_neat.banking_backend.reporting;

import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.banking_backend.approvals.GovernanceClient;
import robert_neat.banking_backend.common.InvalidStageTransitionException;
import robert_neat.banking_backend.common.NotFoundException;

/**
 * F7 mutating flow — regulatory report submission and status refresh, proxied through
 * external_institutions_gateway_mock's /regulatory-gateway endpoints (see
 * RegulatoryGatewayClient.java — shared with F6's enforcement transfer). ICT-incident-on-outage
 * follows the same callGateway pattern as EnforcementActionsService/CardActionsService.
 *
 * <p>FATCA/CRS decision: the gateway contract also exposes a separate POST /fatca-crs/export (XML
 * generation from customer/taxId/balance records), but RegulatoryReport/banking_web carry no such
 * record-level data — reporting.page.ts has no FATCA/CRS-specific action beyond the recipient
 * filter. /regulatory-gateway/submit already accepts recipient=FATCA/CRS, so that one generic path
 * is used for all four recipients; /fatca-crs/export is intentionally not wired in this round.
 *
 * <p>REJECTED handling decision: RegulatoryReportDto's status union in banking_web
 * (reporting.model.ts) only has draft|submitted|accepted, matching the regulatory_report table's
 * CHECK constraint (draft|submitted|accepted — no 'rejected'). A REJECTED gateway outcome moves the
 * report back to 'draft' so the employee can fix and resubmit; validationErrors from the gateway
 * are returned to the frontend so the reason is visible even though the status itself reverts. Note
 * the gateway's rejection rule is deterministic per reportId (floorMod(reportId. hashCode(),10)==0)
 * — a report that triggers it will be rejected on every resubmission attempt with the same id,
 * which is an intentional demo property, not a bug.
 */
@Service
@Transactional
public class ReportingActionsService {

  private static final Logger log = LoggerFactory.getLogger(ReportingActionsService.class);
  private static final int MAX_VALIDATION_ERRORS_LENGTH = 1000;

  private final RegulatoryReportRepository regulatoryReports;
  private final ReportSubmissionRepository reportSubmissions;
  private final RegulatoryGatewayClient gatewayClient;
  private final GovernanceClient governanceClient;

  public ReportingActionsService(
      RegulatoryReportRepository regulatoryReports,
      ReportSubmissionRepository reportSubmissions,
      RegulatoryGatewayClient gatewayClient,
      GovernanceClient governanceClient) {
    this.regulatoryReports = regulatoryReports;
    this.reportSubmissions = reportSubmissions;
    this.gatewayClient = gatewayClient;
    this.governanceClient = governanceClient;
  }

  public RegulatoryReportDto submit(String reportId, String employeeId) {
    RegulatoryReport report = requireReport(reportId);
    if (!"draft".equals(report.getStatus())) {
      throw new InvalidStageTransitionException(
          "Regulatory report "
              + reportId
              + " is not in status 'draft' (current: "
              + report.getStatus()
              + ")");
    }

    GatewaySubmissionResponse response =
        callGateway(
            "submit",
            reportId,
            () ->
                gatewayClient.submit(
                    report.getId(), report.getRecipient(), report.getName(), report.getPeriod()));

    ReportSubmission submission = new ReportSubmission();
    submission.setRegulatoryReport(report);
    submission.setGatewaySubmissionId(response.submissionId());
    submission.setGatewayStatus(response.status());
    submission.setValidationErrors(joinValidationErrors(response.validationErrors()));
    submission.setSubmittedAt(
        response.submittedAt() != null ? response.submittedAt() : Instant.now());
    reportSubmissions.save(submission);

    report.setStatus("submitted");
    regulatoryReports.save(report);

    governanceClient.recordAuditEvent(
        employeeId,
        "REPORT_SUBMITTED",
        "REGULATORY_REPORT",
        reportId,
        "gatewaySubmissionId=" + response.submissionId());

    return toDto(report);
  }

  public RefreshSubmissionStatusResponse refreshSubmissionStatus(
      String reportId, String employeeId) {
    RegulatoryReport report = requireReport(reportId);
    if (!"submitted".equals(report.getStatus())) {
      throw new InvalidStageTransitionException(
          "Regulatory report "
              + reportId
              + " is not in status 'submitted' (current: "
              + report.getStatus()
              + ")");
    }
    ReportSubmission submission =
        reportSubmissions
            .findFirstByRegulatoryReportIdOrderBySubmittedAtDesc(reportId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "No submission recorded yet for regulatory report: " + reportId));

    GatewaySubmissionResponse response =
        callGateway(
            "refresh-submission-status",
            reportId,
            () -> gatewayClient.getSubmissionStatus(submission.getGatewaySubmissionId()));

    submission.setGatewayStatus(response.status());
    submission.setValidationErrors(joinValidationErrors(response.validationErrors()));
    if (!"PENDING".equals(response.status())) {
      submission.setResolvedAt(
          response.resolvedAt() != null ? response.resolvedAt() : Instant.now());
    }
    reportSubmissions.save(submission);

    if ("ACCEPTED".equals(response.status())) {
      report.setStatus("accepted");
      regulatoryReports.save(report);
    } else if ("REJECTED".equals(response.status())) {
      report.setStatus("draft"); // see class javadoc — TS model has no 'rejected' status
      regulatoryReports.save(report);
    }

    governanceClient.recordAuditEvent(
        employeeId,
        "REPORT_SUBMISSION_STATUS_REFRESHED",
        "REGULATORY_REPORT",
        reportId,
        "gatewayStatus=" + response.status());

    return new RefreshSubmissionStatusResponse(toDto(report), toDto(submission));
  }

  private String joinValidationErrors(List<String> errors) {
    if (errors == null || errors.isEmpty()) {
      return null;
    }
    String joined = String.join("; ", errors);
    return joined.length() > MAX_VALIDATION_ERRORS_LENGTH
        ? joined.substring(0, MAX_VALIDATION_ERRORS_LENGTH)
        : joined;
  }

  /** Mirrors CardActionsService.callProcessor()/EnforcementActionsService.callGateway(). */
  private <T> T callGateway(
      String operation, String reportId, java.util.function.Supplier<T> call) {
    try {
      return call.get();
    } catch (RegulatoryGatewayUnavailableException ex) {
      reportIctIncident(operation, reportId, ex);
      throw ex;
    }
  }

  private void reportIctIncident(
      String operation, String reportId, RegulatoryGatewayUnavailableException cause) {
    try {
      governanceClient.createIctIncident(
          "EXTERNAL_INSTITUTIONS_GATEWAY",
          "external_institutions_gateway_mock unavailable during "
              + operation
              + " for report "
              + reportId
              + ": "
              + cause.getMessage(),
          "HIGH");
    } catch (RuntimeException ictFailure) {
      log.warn(
          "Failed to record ICT incident for gateway outage ({}): {}",
          operation,
          ictFailure.getMessage());
    }
  }

  private RegulatoryReport requireReport(String id) {
    return regulatoryReports
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Regulatory report not found: " + id));
  }

  static RegulatoryReportDto toDto(RegulatoryReport r) {
    return new RegulatoryReportDto(
        r.getId(), r.getRecipient(), r.getName(), r.getPeriod(), r.getDueDate(), r.getStatus());
  }

  static ReportSubmissionDto toDto(ReportSubmission s) {
    return new ReportSubmissionDto(
        s.getGatewaySubmissionId(),
        s.getGatewayStatus(),
        s.getValidationErrors(),
        s.getSubmittedAt(),
        s.getResolvedAt());
  }
}
