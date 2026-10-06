import { Component, computed, inject } from '@angular/core';
import { ChartModule } from 'primeng/chart';
import { TableModule } from 'primeng/table';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { TranslationService } from '../../core/services/translation.service';
import { ThemeService } from '../../core/services/theme.service';
import { KpiCardComponent } from '../../shared/components/kpi-card.component';
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { CASE_SEVERITY } from '../cases/cases.model';
import { REPORT_SEVERITY } from '../reporting/reporting.model';
import { RECENT_CASES, REGULATORY_ALERTS } from './dashboard.data';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [ChartModule, TableModule, TranslatePipe, KpiCardComponent, StatusTagComponent],
  templateUrl: './dashboard.page.html',
})
export class DashboardPage {
  private readonly theme = inject(ThemeService);
  private readonly i18n = inject(TranslationService);

  protected readonly recentCases = RECENT_CASES;
  protected readonly regulatoryAlerts = REGULATORY_ALERTS;
  protected readonly caseSeverity = CASE_SEVERITY;
  protected readonly reportSeverity = REPORT_SEVERITY;

  private readonly textColor = computed(() =>
    this.theme.mode() === 'dark' ? '#c3c8d1' : '#4c5666',
  );
  private readonly gridColor = computed(() =>
    this.theme.mode() === 'dark' ? '#282d35' : '#eceef1',
  );

  protected readonly trendData = computed(() => ({
    labels: ['Sty', 'Lut', 'Mar', 'Kwi', 'Maj', 'Cze', 'Lip', 'Sie', 'Wrz'],
    datasets: [
      {
        label: this.i18n.translate('dashboard.kpi.loansValue'),
        data: [412, 428, 441, 455, 468, 479, 491, 503, 512],
        fill: true,
        tension: 0.35,
        borderColor: '#c15f1e',
        backgroundColor: 'rgba(193, 95, 30, 0.14)',
        pointBackgroundColor: '#c15f1e',
        pointRadius: 3,
        borderWidth: 2,
      },
    ],
  }));

  protected readonly trendOptions = computed(() => ({
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      x: { ticks: { color: this.textColor() }, grid: { color: 'transparent' } },
      y: { ticks: { color: this.textColor() }, grid: { color: this.gridColor() } },
    },
  }));

  protected readonly productData = computed(() => ({
    labels: [
      this.i18n.translate('loans.product.mortgage'),
      this.i18n.translate('loans.product.cash'),
      this.i18n.translate('loans.product.investment'),
      this.i18n.translate('loans.product.creditLine'),
      this.i18n.translate('loans.product.overdraft'),
    ],
    datasets: [
      {
        data: [38, 24, 19, 12, 7],
        backgroundColor: ['#2a78d6', '#eb6834', '#1baf7a', '#eda100', '#4a3aa7'],
        borderWidth: 0,
        borderRadius: 4,
      },
    ],
  }));

  protected readonly productOptions = computed(() => ({
    indexAxis: 'y' as const,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      x: { ticks: { color: this.textColor() }, grid: { color: this.gridColor() } },
      y: { ticks: { color: this.textColor() }, grid: { color: 'transparent' } },
    },
  }));
}
