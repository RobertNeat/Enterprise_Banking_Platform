package robert_neat.banking_backend.reporting;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reporting")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/reports")
    public List<RegulatoryReportDto> findAll() {
        return reportingService.findAll();
    }

    @GetMapping("/reports/{id}/submissions")
    public List<ReportSubmissionDto> findSubmissions(@PathVariable String id) {
        return reportingService.findSubmissions(id);
    }
}
