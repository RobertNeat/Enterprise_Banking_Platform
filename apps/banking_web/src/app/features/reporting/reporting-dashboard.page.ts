import { Component, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { ChartModule } from 'primeng/chart';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { TranslationService } from '../../core/services/translation.service';
import { ThemeService } from '../../core/services/theme.service';
import { KpiCardComponent } from '../../shared/components/kpi-card.component';
import { ReportingStore } from './reporting.store';
import { ReportStatus, RegulatoryReport } from './reporting.model';

// Fixed "today" reference, consistent with loans.data.ts buildSchedule — keeps
// KPI figures stable regardless of when the app is opened.
const TODAY = '2026-10-05';

const RECIPIENTS: RegulatoryReport['recipient'][] = ['KNF', 'NBP', 'BFG', 'FATCA/CRS'];
const STATUSES: ReportStatus[] = ['draft', 'submitted', 'accepted'];

@Component({
  selector: 'app-reporting-dashboard-page',
  standalone: true,
  imports: [ButtonModule, ChartModule, TranslatePipe, KpiCardComponent],
  templateUrl: './reporting-dashboard.page.html',
})
export class ReportingDashboardPage {
  private readonly router = inject(Router);
  private readonly theme = inject(ThemeService);
  private readonly i18n = inject(TranslationService);
  private readonly store = inject(ReportingStore);

  protected readonly reports = this.store.reports;

  protected readonly totalCount = computed(() => this.reports().length);
  protected readonly acceptedCount = computed(
    () => this.reports().filter((r) => r.status === 'accepted').length,
  );
  protected readonly overdueCount = computed(
    () => this.reports().filter((r) => r.status !== 'accepted' && r.dueDate < TODAY).length,
  );
  protected readonly nextDueDate = computed(() => {
    const upcoming = this.reports()
      .filter((r) => r.status !== 'accepted' && r.dueDate >= TODAY)
      .map((r) => r.dueDate)
      .sort();
    return upcoming[0] ?? '—';
  });

  // Light/dark steps of categorical slot 1 (blue) — single-series bar needs no legend.
  private readonly barColor = computed(() =>
    this.theme.mode() === 'dark' ? '#3987e5' : '#2a78d6',
  );
  // Categorical slots 1-3 — the only three that pass the all-pairs CVD/contrast
  // gate in both modes (see dataviz skill palette.md); doughnut legend carries
  // the labels so the light-mode aqua contrast WARN is mitigated. Order
  // (draft=orange, submitted=blue, accepted=aqua) mirrors the info/success
  // semantics of REPORT_SEVERITY on the list page's status tags, so blue
  // doesn't mean "draft" here and "submitted" there.
  private readonly statusColors = computed(() =>
    this.theme.mode() === 'dark'
      ? ['#d95926', '#3987e5', '#199e70']
      : ['#eb6834', '#2a78d6', '#1baf7a'],
  );
  private readonly surfaceColor = computed(() =>
    this.theme.mode() === 'dark' ? '#1a1a19' : '#fcfcfb',
  );
  private readonly textColor = computed(() =>
    this.theme.mode() === 'dark' ? '#c3c8d1' : '#4c5666',
  );
  private readonly gridColor = computed(() =>
    this.theme.mode() === 'dark' ? '#282d35' : '#eceef1',
  );

  protected readonly recipientData = computed(() => ({
    labels: RECIPIENTS,
    datasets: [
      {
        label: this.i18n.translate('reporting.dashboard.byRecipient'),
        data: RECIPIENTS.map(
          (recipient) => this.reports().filter((r) => r.recipient === recipient).length,
        ),
        backgroundColor: this.barColor(),
        borderRadius: 4,
      },
    ],
  }));

  protected readonly recipientOptions = computed(() => ({
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      x: { ticks: { color: this.textColor() }, grid: { color: 'transparent' } },
      y: {
        beginAtZero: true,
        ticks: { color: this.textColor(), precision: 0 },
        grid: { color: this.gridColor() },
      },
    },
  }));

  protected readonly statusData = computed(() => ({
    labels: STATUSES.map((status) => this.i18n.translate(`reporting.status.${status}`)),
    datasets: [
      {
        data: STATUSES.map((status) => this.reports().filter((r) => r.status === status).length),
        backgroundColor: this.statusColors(),
        borderWidth: 2,
        borderColor: this.surfaceColor(),
      },
    ],
  }));

  protected readonly statusOptions = computed(() => ({
    maintainAspectRatio: false,
    plugins: { legend: { position: 'bottom' as const, labels: { color: this.textColor() } } },
  }));

  back(): void {
    this.router.navigate(['/reporting']);
  }
}
