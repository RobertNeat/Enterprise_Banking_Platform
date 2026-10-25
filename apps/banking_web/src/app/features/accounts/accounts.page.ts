import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { NotificationService } from '../../core/services/notification.service';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { ACCOUNT_STATUS_SEVERITY, ASSIGNABLE_ROLES, EmployeeAccountView } from './accounts.model';
import { AccountsStore } from './accounts.store';

@Component({
  selector: 'app-accounts-page',
  standalone: true,
  imports: [
    ButtonModule,
    DialogModule,
    FormsModule,
    SelectModule,
    TableModule,
    TranslatePipe,
    StatusTagComponent,
  ],
  templateUrl: './accounts.page.html',
})
export class AccountsPage {
  private readonly store = inject(AccountsStore);
  private readonly notify = inject(NotificationService);

  protected readonly accounts = this.store.accounts;
  protected readonly severity = ACCOUNT_STATUS_SEVERITY;
  protected readonly assignableRoles = ASSIGNABLE_ROLES;

  protected readonly pendingAccounts = computed(() =>
    this.accounts().filter((a) => a.status === 'PENDING_APPROVAL'),
  );
  protected readonly decidedAccounts = computed(() =>
    this.accounts().filter((a) => a.status !== 'PENDING_APPROVAL'),
  );

  protected readonly approveDialogOpen = signal(false);
  protected readonly approvingAccount = signal<EmployeeAccountView | null>(null);
  protected readonly selectedRole = signal<string | null>(null);
  protected readonly busyId = signal<string | null>(null);

  constructor() {
    this.store.load().catch((error: unknown) => this.notify.error(this.resolveErrorKey(error)));
  }

  openApproveDialog(account: EmployeeAccountView): void {
    this.approvingAccount.set(account);
    this.selectedRole.set(null);
    this.approveDialogOpen.set(true);
  }

  closeApproveDialog(): void {
    this.approveDialogOpen.set(false);
    this.approvingAccount.set(null);
  }

  async confirmApprove(): Promise<void> {
    const account = this.approvingAccount();
    const role = this.selectedRole();
    if (!account || !role) return;
    this.busyId.set(account.id);
    try {
      await this.store.approve(account.id, role);
      this.notify.success('toast.saved');
      this.closeApproveDialog();
    } catch (error) {
      this.notify.error(this.resolveErrorKey(error));
    } finally {
      this.busyId.set(null);
    }
  }

  async reject(account: EmployeeAccountView): Promise<void> {
    this.busyId.set(account.id);
    try {
      await this.store.reject(account.id);
      this.notify.success('toast.saved');
    } catch (error) {
      this.notify.error(this.resolveErrorKey(error));
    } finally {
      this.busyId.set(null);
    }
  }

  private resolveErrorKey(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 403) {
      return 'accounts.error.forbidden';
    }
    if (error instanceof HttpErrorResponse && error.status === 409) {
      return 'accounts.error.notPending';
    }
    return 'accounts.error.generic';
  }
}
