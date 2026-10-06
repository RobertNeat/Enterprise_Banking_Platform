import { Component, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { NotificationService } from '../../core/services/notification.service';
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { NEUTRAL_TAG_SEVERITY } from '../../shared/utils/status-severity';
import { EBANKING_USERS } from './ebanking.data';
import { ACCESS_SEVERITY, EbankingUser } from './ebanking.model';

@Component({
  selector: 'app-ebanking-page',
  standalone: true,
  imports: [DecimalPipe, ButtonModule, TableModule, TranslatePipe, StatusTagComponent],
  templateUrl: './ebanking.page.html',
})
export class EbankingPage {
  private readonly notify = inject(NotificationService);

  protected readonly users = EBANKING_USERS;
  protected readonly accessSeverity = ACCESS_SEVERITY;
  protected readonly neutralSeverity = NEUTRAL_TAG_SEVERITY;

  unlock(user: EbankingUser): void {
    if (user.accessStatus === 'active') {
      this.notify.warn('toast.actionBlocked');
      return;
    }
    user.accessStatus = 'active';
    this.notify.success('toast.saved');
  }

  resetDevice(user: EbankingUser): void {
    this.notify.info('ebanking.resetDevice');
  }
}
