import { Component, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { NotificationService } from '../../core/services/notification.service';
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { ENFORCEMENT_CASES } from './enforcement.data';
import { ENFORCEMENT_SEVERITY, EnforcementCase } from './enforcement.model';

@Component({
  selector: 'app-enforcement-page',
  standalone: true,
  imports: [DecimalPipe, ButtonModule, TableModule, TranslatePipe, StatusTagComponent],
  templateUrl: './enforcement.page.html',
})
export class EnforcementPage {
  private readonly notify = inject(NotificationService);

  protected readonly cases = ENFORCEMENT_CASES;
  protected readonly severity = ENFORCEMENT_SEVERITY;

  transfer(item: EnforcementCase): void {
    if (item.status !== 'active') {
      this.notify.warn('toast.actionBlocked');
      return;
    }
    this.notify.success('toast.saved');
  }
}
