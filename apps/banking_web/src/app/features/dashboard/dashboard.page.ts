import { Component, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ChartModule } from 'primeng/chart';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { firstValueFrom } from 'rxjs';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { TranslationService } from '../../core/services/translation.service';
import { ThemeService } from '../../core/services/theme.service';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { NotificationService } from '../../core/services/notification.service';
import { KpiCardComponent } from '../../shared/components/kpi-card.component';
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { formatCount, formatPercent, numberFormat } from '../../shared/utils/format-number';
import { CASE_SEVERITY } from '../cases/cases.model';
import { REPORT_SEVERITY } from '../reporting/reporting.model';
import { LOAN_PRODUCTS, LoanApplication } from '../loans/loans.model';
import { LoansStore } from '../loans/loans.store';
import {
  ICT_INCIDENT_SEVERITY,
  IctIncidentAlert,
  KpiSnapshot,
  PortfolioTrendPoint,
  RecentCase,
  RegulatoryAlert,
} from './dashboard.data';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [
    ChartModule,
    TableModule,
    ButtonModule,
    TranslatePipe,
    KpiCardComponent,
    StatusTagComponent,
  ],
  templateUrl: './dashboard.page.html',
})
export class DashboardPage {
  private readonly theme = inject(ThemeService);
  private readonly i18n = inject(TranslationService);
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(ApiBaseUrlService);
  private readonly loansStore = inject(LoansStore);
  private readonly notify = inject(NotificationService);

  private readonly recentCasesSignal = signal<RecentCase[]>([]);
  private readonly regulatoryAlertsSignal = signal<RegulatoryAlert[]>([]);
  private readonly ictIncidentsSignal = signal<IctIncidentAlert[]>([]);
  private readonly kpiSnapshotSignal = signal<KpiSnapshot | null>(null);
  private readonly portfolioTrendSignal = signal<PortfolioTrendPoint[]>([]);

  protected readonly recentCases = this.recentCasesSignal.asReadonly();
  protected readonly regulatoryAlerts = this.regulatoryAlertsSignal.asReadonly();
  protected readonly ictIncidents = this.ictIncidentsSignal.asReadonly();
  protected readonly kpiSnapshot = this.kpiSnapshotSignal.asReadonly();
  protected readonly caseSeverity = CASE_SEVERITY;
  protected readonly reportSeverity = REPORT_SEVERITY;
  protected readonly ictIncidentSeverity = ICT_INCIDENT_SEVERITY;

  constructor() {
    this.http
      .get<RecentCase[]>(`${this.apiBaseUrl.bankingBackend}/dashboard/recent-cases`)
      .subscribe((cases) => this.recentCasesSignal.set(cases));
    this.http
      .get<RegulatoryAlert[]>(`${this.apiBaseUrl.bankingBackend}/dashboard/regulatory-alerts`)
      .subscribe((alerts) => this.regulatoryAlertsSignal.set(alerts));
    this.fetchIctIncidents();
    // 404 when no snapshot is seeded (DashboardService.findLatestKpiSnapshot) — tiles keep
    // showing the '—' placeholder rather than surfacing an unhandled error.
    this.http
      .get<KpiSnapshot>(`${this.apiBaseUrl.bankingBackend}/dashboard/kpi-snapshot/latest`)
      .subscribe({
        next: (snapshot) => this.kpiSnapshotSignal.set(snapshot),
        error: () => this.kpiSnapshotSignal.set(null),
      });
    this.http
      .get<PortfolioTrendPoint[]>(`${this.apiBaseUrl.bankingBackend}/dashboard/portfolio-trend`)
      .subscribe((points) => this.portfolioTrendSignal.set(points));
  }

  private fetchIctIncidents(): void {
    this.http
      .get<IctIncidentAlert[]>(`${this.apiBaseUrl.bankingBackend}/dashboard/ict-incidents`)
      .subscribe((incidents) => this.ictIncidentsSignal.set(incidents));
  }

  /**
   * DSH-05 — every row already shown here is OPEN (DashboardService.findIctIncidentAlerts
   * filters server-side), so the resolved incident simply drops off the list on refetch.
   */
  async resolveIctIncident(incident: IctIncidentAlert): Promise<void> {
    try {
      await firstValueFrom(
        this.http.post(
          `${this.apiBaseUrl.bankingBackend}/dashboard/ict-incidents/${incident.id}/resolve`,
          {},
        ),
      );
      this.notify.success('toast.saved');
      this.fetchIctIncidents();
    } catch {
      this.notify.error('dashboard.ictIncidents.error.resolveFailed');
    }
  }

  private readonly textColor = computed(() =>
    this.theme.mode() === 'dark' ? '#c3c8d1' : '#4c5666',
  );
  private readonly gridColor = computed(() =>
    this.theme.mode() === 'dark' ? '#282d35' : '#eceef1',
  );

  private millions(value: number): string {
    const formatted = numberFormat({
      maximumFractionDigits: 1,
      minimumFractionDigits: 1,
      useGrouping: 'always',
    }).format(value / 1_000_000);
    return `${formatted} ${this.i18n.translate('dashboard.kpi.unit.millionsPln')}`;
  }

  private count(value: number): string {
    return formatCount(value);
  }

  private percent(value: number, fractionDigits = 1): string {
    return formatPercent(value, fractionDigits);
  }

  protected readonly kpiLoansActive = computed(() => {
    const snapshot = this.kpiSnapshot();
    return snapshot ? this.count(snapshot.activeLoansCount) : '—';
  });

  protected readonly kpiLoansValue = computed(() => {
    const snapshot = this.kpiSnapshot();
    return snapshot ? this.millions(snapshot.loanPortfolioTotal) : '—';
  });

  protected readonly kpiCardsIssued = computed(() => {
    const snapshot = this.kpiSnapshot();
    return snapshot ? this.count(snapshot.cardsIssuedCount) : '—';
  });

  protected readonly kpiNpl = computed(() => {
    const snapshot = this.kpiSnapshot();
    return snapshot ? this.percent(snapshot.nplRatio) : '—';
  });

  protected readonly kpiLcr = computed(() => {
    const snapshot = this.kpiSnapshot();
    return snapshot ? this.percent(snapshot.lcrRatio, 0) : '—';
  });

  protected readonly kpiNsfr = computed(() => {
    const snapshot = this.kpiSnapshot();
    return snapshot ? this.percent(snapshot.nsfrRatio, 0) : '—';
  });

  protected readonly kpiCasesOpen = computed(() => {
    const snapshot = this.kpiSnapshot();
    return snapshot ? this.count(snapshot.openCasesCount) : '—';
  });

  protected readonly kpiCollectionsOverdue = computed(() => {
    const snapshot = this.kpiSnapshot();
    return snapshot ? this.millions(snapshot.collectionsOverdueTotal) : '—';
  });

  // GET /dashboard/portfolio-trend already sorts by snapshotDate asc (DashboardService).
  protected readonly trendData = computed(() => {
    const points = this.portfolioTrend();
    const locale = this.i18n.lang() === 'pl' ? 'pl-PL' : 'en-US';
    const monthFormatter = new Intl.DateTimeFormat(locale, { month: 'short' });
    return {
      labels: points.map((point) => {
        const label = monthFormatter.format(new Date(point.snapshotDate));
        return label.charAt(0).toUpperCase() + label.slice(1);
      }),
      datasets: [
        {
          label: this.i18n.translate('dashboard.kpi.loansValue'),
          data: points.map((point) => point.portfolioTotal / 1_000_000),
          fill: true,
          tension: 0.35,
          borderColor: '#c15f1e',
          backgroundColor: 'rgba(193, 95, 30, 0.14)',
          pointBackgroundColor: '#c15f1e',
          pointRadius: 3,
          borderWidth: 2,
        },
      ],
    };
  });

  protected readonly portfolioTrend = this.portfolioTrendSignal.asReadonly();

  protected readonly trendOptions = computed(() => ({
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      x: { ticks: { color: this.textColor() }, grid: { color: 'transparent' } },
      y: { ticks: { color: this.textColor() }, grid: { color: this.gridColor() } },
    },
  }));

  // Aggregates GET /loans by product on the frontend (backend has no such endpoint). Scale
  // deliberately differs from the KPI snapshot (13 seeded applications vs. tens of thousands in
  // dashboard_kpi_snapshot) — see banking_backend.md pkt 22; not reconciled on purpose.
  private readonly loanProducts = computed(() =>
    this.loansStore.applications().reduce(
      (counts, loan: LoanApplication) => {
        counts[loan.product] = (counts[loan.product] ?? 0) + 1;
        return counts;
      },
      {} as Record<string, number>,
    ),
  );

  protected readonly productData = computed(() => {
    const counts = this.loanProducts();
    return {
      labels: LOAN_PRODUCTS.map((product) => this.i18n.translate(`loans.product.${product}`)),
      datasets: [
        {
          data: LOAN_PRODUCTS.map((product) => counts[product] ?? 0),
          backgroundColor: ['#2a78d6', '#eb6834', '#1baf7a', '#eda100', '#4a3aa7'],
          borderWidth: 0,
          borderRadius: 4,
        },
      ],
    };
  });

  protected readonly productOptions = computed(() => ({
    indexAxis: 'y' as const,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      x: {
        beginAtZero: true,
        ticks: { color: this.textColor(), precision: 0 },
        grid: { color: this.gridColor() },
      },
      y: { ticks: { color: this.textColor() }, grid: { color: 'transparent' } },
    },
  }));
}
