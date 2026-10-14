package robert_neat.banking_backend.dashboard;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import robert_neat.banking_backend.cases.CustomerCaseRepository;
import robert_neat.banking_backend.common.EmployeeNames;
import robert_neat.banking_backend.common.NotFoundException;
import robert_neat.banking_backend.reporting.RegulatoryReportRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final int RECENT_CASES_LIMIT = 4;
    private static final int REGULATORY_ALERTS_LIMIT = 3;

    private final CustomerCaseRepository customerCases;
    private final RegulatoryReportRepository regulatoryReports;
    private final DashboardKpiSnapshotRepository kpiSnapshots;
    private final PortfolioTrendPointRepository trendPoints;

    public DashboardService(
            CustomerCaseRepository customerCases,
            RegulatoryReportRepository regulatoryReports,
            DashboardKpiSnapshotRepository kpiSnapshots,
            PortfolioTrendPointRepository trendPoints) {
        this.customerCases = customerCases;
        this.regulatoryReports = regulatoryReports;
        this.kpiSnapshots = kpiSnapshots;
        this.trendPoints = trendPoints;
    }

    public List<RecentCaseDto> findRecentCases() {
        return customerCases.findByOrderByRegisteredAtDesc(PageRequest.of(0, RECENT_CASES_LIMIT)).stream()
                .map(c -> new RecentCaseDto(
                        c.getId(),
                        c.getSubject(),
                        c.getCustomer().getFullName(),
                        c.getStatus(),
                        EmployeeNames.abbreviated(c.getOwner())))
                .toList();
    }

    public List<RegulatoryAlertDto> findRegulatoryAlerts() {
        return regulatoryReports
                .findByStatusNotOrderByDueDateAsc("accepted", PageRequest.of(0, REGULATORY_ALERTS_LIMIT))
                .stream()
                .map(r -> new RegulatoryAlertDto(r.getId(), r.getRecipient(), r.getStatus(), r.getDueDate()))
                .toList();
    }

    public DashboardKpiSnapshotDto findLatestKpiSnapshot() {
        DashboardKpiSnapshot snapshot = kpiSnapshots.findTopByOrderBySnapshotDateDesc()
                .orElseThrow(() -> new NotFoundException("No dashboard KPI snapshot available"));
        return new DashboardKpiSnapshotDto(
                snapshot.getSnapshotDate(),
                snapshot.getActiveLoansCount(),
                snapshot.getLoanPortfolioTotal(),
                snapshot.getCardsIssuedCount(),
                snapshot.getNplRatio(),
                snapshot.getLcrRatio(),
                snapshot.getNsfrRatio(),
                snapshot.getOpenCasesCount(),
                snapshot.getCollectionsOverdueTotal());
    }

    public List<PortfolioTrendPointDto> findPortfolioTrend() {
        return trendPoints.findByOrderBySnapshotDateAsc().stream()
                .map(p -> new PortfolioTrendPointDto(p.getSnapshotDate(), p.getPortfolioTotal()))
                .toList();
    }
}
