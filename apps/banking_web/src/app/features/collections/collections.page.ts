import { Component, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { NotificationService } from '../../core/services/notification.service';
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { CollectionsStore } from './collections.store';
import { COLLECTIONS_SEVERITY, CollectionsCase } from './collections.model';

@Component({
  selector: 'app-collections-page',
  standalone: true,
  imports: [DecimalPipe, ButtonModule, TableModule, TranslatePipe, StatusTagComponent],
  templateUrl: './collections.page.html',
})
export class CollectionsPage {
  private readonly notify = inject(NotificationService);
  private readonly store = inject(CollectionsStore);

  protected readonly cases = this.store.cases;
  protected readonly severity = COLLECTIONS_SEVERITY;

  sendReminder(item: CollectionsCase): void {
    if (item.stage === 'legal') {
      this.notify.warn('toast.actionBlocked');
      return;
    }
    this.notify.success('toast.saved');
  }

  proposeSettlement(item: CollectionsCase): void {
    this.store.update(item.id, (c) => ({ ...c, stage: 'settlement' }));
    this.notify.success('toast.saved');
  }

  transferExternal(item: CollectionsCase): void {
    if (item.stage === 'monitoring' || item.stage === 'reminder') {
      this.notify.warn('toast.actionBlocked');
      return;
    }
    this.store.update(item.id, (c) => ({ ...c, stage: 'external' }));
    this.notify.success('toast.saved');
  }
}
