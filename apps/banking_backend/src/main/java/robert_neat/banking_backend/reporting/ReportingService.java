package robert_neat.banking_backend.reporting;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.banking_backend.common.NotFoundException;

@Service
@Transactional(readOnly = true)
public class ReportingService {

  private final RegulatoryReportRepository regulatoryReports;
  private final ReportSubmissionRepository reportSubmissions;

  public ReportingService(
      RegulatoryReportRepository regulatoryReports, ReportSubmissionRepository reportSubmissions) {
    this.regulatoryReports = regulatoryReports;
    this.reportSubmissions = reportSubmissions;
  }

  public List<RegulatoryReportDto> findAll() {
    return regulatoryReports.findAll().stream()
        .map(
            r ->
                new RegulatoryReportDto(
                    r.getId(),
                    r.getRecipient(),
                    r.getName(),
                    r.getPeriod(),
                    r.getDueDate(),
                    r.getStatus()))
        .toList();
  }

  public List<ReportSubmissionDto> findSubmissions(String reportId) {
    if (!regulatoryReports.existsById(reportId)) {
      throw new NotFoundException("Regulatory report not found: " + reportId);
    }
    return reportSubmissions.findAll().stream()
        .filter(s -> s.getRegulatoryReport().getId().equals(reportId))
        .map(
            s ->
                new ReportSubmissionDto(
                    s.getGatewaySubmissionId(),
                    s.getGatewayStatus(),
                    s.getValidationErrors(),
                    s.getSubmittedAt(),
                    s.getResolvedAt()))
        .toList();
  }
}
