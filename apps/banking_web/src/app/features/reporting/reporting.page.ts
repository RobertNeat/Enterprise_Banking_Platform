import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { TranslationService } from '../../core/services/translation.service';
import { NotificationService } from '../../core/services/notification.service';
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { ReportingStore } from './reporting.store';
import { GATEWAY_SUBMISSION_SEVERITY, REPORT_SEVERITY, ReportSubmission } from './reporting.model';
import { buildReportsCsv } from './reporting.csv';

@Component({
  selector: 'app-reporting-page',
  standalone: true,
  imports: [
    FormsModule,
    ButtonModule,
    DialogModule,
    SelectModule,
    TableModule,
    TranslatePipe,
    StatusTagComponent,
  ],
  templateUrl: './reporting.page.html',
})
export class ReportingPage {
  private readonly notify = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly store = inject(ReportingStore);
  protected readonly i18n = inject(TranslationService);

  protected readonly reports = this.store.reports;
  protected readonly severity = REPORT_SEVERITY;
  protected readonly submissionSeverity = GATEWAY_SUBMISSION_SEVERITY;
  protected readonly recipientFilter = signal<string>('all');

  protected readonly recipientOptions = [
    { label: 'Wszyscy', value: 'all' },
    { label: 'KNF', value: 'KNF' },
    { label: 'NBP', value: 'NBP' },
    { label: 'BFG', value: 'BFG' },
    { label: 'FATCA/CRS', value: 'FATCA/CRS' },
  ];

  exportCsv(): void {
    const csv = buildReportsCsv(this.reports(), this.recipientFilter(), (status) =>
      this.i18n.translate(`reporting.status.${status}`),
    );
    // eslint-disable-next-line no-irregular-whitespace -- intentional UTF-8 BOM so Excel detects the encoding
    const blob = new Blob([`﻿${csv}`], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'raporty-regulacyjne.csv';
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 0);
    this.notify.success('toast.exported');
  }

  viewDashboard(): void {
    this.router.navigate(['/reporting/dashboard']);
  }

  /** F7 — submits a 'draft' report to KNF/NBP/BFG/FATCA-CRS via the gateway. */
  async submit(report: { id: string; status: string }): Promise<void> {
    if (report.status !== 'draft') {
      this.notify.warn('toast.actionBlocked');
      return;
    }
    try {
      await this.store.submit(report.id);
      this.notify.success('toast.saved');
    } catch {
      this.notify.error('reporting.error.generic');
    }
  }

  /** F7 — checks the gateway's accept/reject outcome for a 'submitted' report. */
  async checkStatus(report: { id: string; status: string }): Promise<void> {
    if (report.status !== 'submitted') {
      this.notify.warn('toast.actionBlocked');
      return;
    }
    try {
      const response = await this.store.refreshSubmissionStatus(report.id);
      if (response.submission.gatewayStatus === 'PENDING') {
        this.notify.info('reporting.statusStillPending');
      } else if (response.submission.gatewayStatus === 'REJECTED') {
        this.notify.warn('reporting.statusRejected', {
          reason: response.submission.validationErrors ?? '',
        });
      } else {
        this.notify.success('toast.saved');
      }
    } catch {
      this.notify.error('reporting.error.generic');
    }
  }

  // --- Historia zgłoszeń do bramy (GET /reporting/reports/{id}/submissions) ---

  protected readonly submissionsDialogOpen = signal(false);
  protected readonly submissionsLoading = signal(false);
  protected readonly submissions = signal<ReportSubmission[]>([]);

  async openSubmissionsDialog(report: { id: string }): Promise<void> {
    this.submissionsDialogOpen.set(true);
    this.submissionsLoading.set(true);
    try {
      const result = await this.store.findSubmissions(report.id);
      this.submissions.set(result);
    } catch {
      this.submissions.set([]);
      this.notify.error('reporting.submissionsHistory.error');
    } finally {
      this.submissionsLoading.set(false);
    }
  }

  closeSubmissionsDialog(): void {
    this.submissionsDialogOpen.set(false);
    this.submissions.set([]);
  }
}
