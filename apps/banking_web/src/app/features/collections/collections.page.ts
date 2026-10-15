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

  async sendReminder(item: CollectionsCase): Promise<void> {
    try {
      await this.store.remind(item.id);
      this.notify.success('toast.saved');
    } catch {
      this.notify.warn('toast.actionBlocked');
    }
  }

  async proposeSettlement(item: CollectionsCase): Promise<void> {
    try {
      await this.store.proposeSettlement(item.id);
      this.notify.success('toast.saved');
    } catch {
      this.notify.warn('toast.actionBlocked');
    }
  }

  async transferExternal(item: CollectionsCase): Promise<void> {
    try {
      await this.store.transferExternal(item.id);
      this.notify.success('toast.saved');
    } catch {
      this.notify.warn('toast.actionBlocked');
    }
  }
}
