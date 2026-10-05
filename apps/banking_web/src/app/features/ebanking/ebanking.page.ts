import { Component, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { NotificationService } from '../../core/services/notification.service';
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { EBANKING_USERS } from './ebanking.data';
import { ACCESS_SEVERITY, EbankingUser } from './ebanking.model';

@Component({
  selector: 'app-ebanking-page',
  standalone: true,
  imports: [
    DecimalPipe,
    ButtonModule,
    ChipModule,
    TableModule,
    TagModule,
    TranslatePipe,
    StatusTagComponent,
  ],
  templateUrl: './ebanking.page.html',
})
export class EbankingPage {
  private readonly notify = inject(NotificationService);

  protected readonly users = EBANKING_USERS;
  protected readonly accessSeverity = ACCESS_SEVERITY;

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
