import { Component, ViewChild, inject, signal } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { Table, TableModule } from 'primeng/table';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { TranslationService } from '../../core/services/translation.service';
import { NotificationService } from '../../core/services/notification.service';
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { REGULATORY_REPORTS } from './reporting.data';
import { REPORT_SEVERITY } from './reporting.model';

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
  protected readonly i18n = inject(TranslationService);

  @ViewChild('dt') table?: Table;

  protected readonly reports = REGULATORY_REPORTS;
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
    this.table?.exportCSV();
    this.notify.success('toast.exported');
  }

  viewDashboard(): void {
    this.notify.info('reporting.viewDashboard');
  }
}
