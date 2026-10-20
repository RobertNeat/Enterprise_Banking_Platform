package robert_neat.banking_backend.dashboard;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import robert_neat.banking_backend.approvals.GovernanceClient;
import robert_neat.banking_backend.approvals.GovernanceIctIncidentResponse;
import robert_neat.banking_backend.cases.CustomerCaseRepository;
import robert_neat.banking_backend.common.EmployeeNames;
import robert_neat.banking_backend.common.NotFoundException;
import robert_neat.banking_backend.reporting.RegulatoryReportRepository;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final int RECENT_CASES_LIMIT = 4;
    private static final int REGULATORY_ALERTS_LIMIT = 3;
    private static final int ICT_INCIDENTS_LIMIT = 5;

    private final CustomerCaseRepository customerCases;
    private final RegulatoryReportRepository regulatoryReports;
    private final DashboardKpiSnapshotRepository kpiSnapshots;
    private final PortfolioTrendPointRepository trendPoints;
    private final GovernanceClient governanceClient;

    public DashboardService(
            CustomerCaseRepository customerCases,
            RegulatoryReportRepository regulatoryReports,
            DashboardKpiSnapshotRepository kpiSnapshots,
            PortfolioTrendPointRepository trendPoints,
            GovernanceClient governanceClient) {
        this.customerCases = customerCases;
        this.regulatoryReports = regulatoryReports;
        this.kpiSnapshots = kpiSnapshots;
        this.trendPoints = trendPoints;
        this.governanceClient = governanceClient;
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

    /**
     * TOP N most recent OPEN ICT incidents (DORA registry) — proxies governance_mock's
     * GET /ict-incidents. Not a DB read, so it opts out of this class's read-only transaction
     * (no reason to hold a connection open for an outbound HTTP call).
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<IctIncidentAlertDto> findIctIncidentAlerts() {
        return governanceClient.listIctIncidents().stream()
                .filter(incident -> "OPEN".equals(incident.status()))
                .sorted(Comparator.comparing(GovernanceIctIncidentResponse::createdAt).reversed())
                .limit(ICT_INCIDENTS_LIMIT)
                .map(incident -> new IctIncidentAlertDto(
                        incident.id(), incident.source(), incident.description(), incident.severity(),
                        incident.createdAt()))
                .toList();
    }
}
