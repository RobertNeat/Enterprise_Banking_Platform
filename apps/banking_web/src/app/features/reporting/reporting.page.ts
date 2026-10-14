import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { TranslationService } from '../../core/services/translation.service';
import { NotificationService } from '../../core/services/notification.service';
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { ReportingStore } from './reporting.store';
import { REPORT_SEVERITY } from './reporting.model';
import { buildReportsCsv } from './reporting.csv';

@Component({
  selector: 'app-reporting-page',
  standalone: true,
  imports: [
    FormsModule,
    ButtonModule,
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
}
