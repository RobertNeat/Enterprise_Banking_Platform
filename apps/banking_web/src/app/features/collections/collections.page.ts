import { Component, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { NotificationService } from '../../core/services/notification.service';
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { COLLECTIONS_CASES } from './collections.data';
import { COLLECTIONS_SEVERITY, CollectionsCase } from './collections.model';

@Component({
  selector: 'app-collections-page',
  standalone: true,
  imports: [DecimalPipe, ButtonModule, TableModule, TranslatePipe, StatusTagComponent],
  templateUrl: './collections.page.html',
})
export class CollectionsPage {
  private readonly notify = inject(NotificationService);

  protected readonly cases = COLLECTIONS_CASES;
  protected readonly severity = COLLECTIONS_SEVERITY;

  sendReminder(item: CollectionsCase): void {
    if (item.stage === 'legal') {
      this.notify.warn('toast.actionBlocked');
      return;
    }
    this.notify.success('toast.saved');
  }

  proposeSettlement(item: CollectionsCase): void {
    item.stage = 'settlement';
    this.notify.success('toast.saved');
  }

  transferExternal(item: CollectionsCase): void {
    if (item.stage === 'monitoring' || item.stage === 'reminder') {
      this.notify.warn('toast.actionBlocked');
      return;
    }
    item.stage = 'external';
    this.notify.success('toast.saved');
  }
}
