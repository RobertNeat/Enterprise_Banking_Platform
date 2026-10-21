package robert_neat.banking_backend.reporting;

import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reporting")
public class ReportingController {

  private final ReportingService reportingService;
  private final ReportingActionsService reportingActionsService;

  public ReportingController(
      ReportingService reportingService, ReportingActionsService reportingActionsService) {
    this.reportingService = reportingService;
    this.reportingActionsService = reportingActionsService;
  }

  @GetMapping("/reports")
  public List<RegulatoryReportDto> findAll() {
    return reportingService.findAll();
  }

  @GetMapping("/reports/{id}/submissions")
  public List<ReportSubmissionDto> findSubmissions(@PathVariable String id) {
    return reportingService.findSubmissions(id);
  }

  /** F7 — submits the report via external_institutions_gateway_mock; 409 unless status 'draft'. */
  @PostMapping("/reports/{id}/submit")
  public RegulatoryReportDto submit(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
    return reportingActionsService.submit(id, jwt.getSubject());
  }

  /** F7 — polls the gateway for the latest submission status; 409 unless status 'submitted'. */
  @PostMapping("/reports/{id}/refresh-submission-status")
  public RefreshSubmissionStatusResponse refreshSubmissionStatus(
      @PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
    return reportingActionsService.refreshSubmissionStatus(id, jwt.getSubject());
  }
}
