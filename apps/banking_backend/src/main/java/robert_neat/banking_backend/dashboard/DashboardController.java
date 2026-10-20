package robert_neat.banking_backend.dashboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/recent-cases")
    public List<RecentCaseDto> findRecentCases() {
        return dashboardService.findRecentCases();
    }

    @GetMapping("/regulatory-alerts")
    public List<RegulatoryAlertDto> findRegulatoryAlerts() {
        return dashboardService.findRegulatoryAlerts();
    }

    @GetMapping("/kpi-snapshot/latest")
    public DashboardKpiSnapshotDto findLatestKpiSnapshot() {
        return dashboardService.findLatestKpiSnapshot();
    }

    @GetMapping("/portfolio-trend")
    public List<PortfolioTrendPointDto> findPortfolioTrend() {
        return dashboardService.findPortfolioTrend();
    }

    @GetMapping("/ict-incidents")
    public List<IctIncidentAlertDto> findIctIncidentAlerts() {
        return dashboardService.findIctIncidentAlerts();
    }
}
