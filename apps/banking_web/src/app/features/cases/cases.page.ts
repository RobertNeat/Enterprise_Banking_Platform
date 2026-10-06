import { Component, inject } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { NotificationService } from '../../core/services/notification.service';
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { PRIORITY_SEVERITY } from '../../shared/utils/status-severity';
import { CUSTOMER_CASES } from './cases.data';
import { CASE_SEVERITY, CustomerCase } from './cases.model';

@Component({
  selector: 'app-cases-page',
  standalone: true,
  imports: [ButtonModule, TableModule, TranslatePipe, StatusTagComponent],
  templateUrl: './cases.page.html',
})
export class CasesPage {
  private readonly notify = inject(NotificationService);

  protected readonly cases = CUSTOMER_CASES;
  protected readonly severity = CASE_SEVERITY;
  protected readonly prioritySeverity = PRIORITY_SEVERITY;

  escalate(item: CustomerCase): void {
    if (item.status === 'resolved') {
      this.notify.warn('toast.actionBlocked');
      return;
    }
    if (item.slaRemainingHours <= 24) {
      this.notify.warn('toast.deadlineClose');
    }
    item.status = 'escalated';
    this.notify.success('toast.saved');
  }
}
