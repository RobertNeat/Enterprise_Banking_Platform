import { Component, inject, signal } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { TableModule } from 'primeng/table';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { NotificationService } from '../../core/services/notification.service';
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { MoneyPipe } from '../../shared/utils/money.pipe';
import { CollectionsStore } from './collections.store';
import { COLLECTIONS_SEVERITY, CollectionsAction, CollectionsCase } from './collections.model';

@Component({
  selector: 'app-collections-page',
  standalone: true,
  imports: [MoneyPipe, ButtonModule, DialogModule, TableModule, TranslatePipe, StatusTagComponent],
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

  // --- Historia akcji windykacyjnych (GET /collections/{id}/actions) ---

  protected readonly actionsDialogOpen = signal(false);
  protected readonly actionsLoading = signal(false);
  protected readonly actions = signal<CollectionsAction[]>([]);

  async openActionsDialog(item: CollectionsCase): Promise<void> {
    this.actionsDialogOpen.set(true);
    this.actionsLoading.set(true);
    try {
      const result = await this.store.findActions(item.id);
      this.actions.set(result);
    } catch {
      this.actions.set([]);
      this.notify.error('collections.actionsHistory.error');
    } finally {
      this.actionsLoading.set(false);
    }
  }

  closeActionsDialog(): void {
    this.actionsDialogOpen.set(false);
    this.actions.set([]);
  }
}
