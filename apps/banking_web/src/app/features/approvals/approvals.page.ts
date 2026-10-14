import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { APPROVAL_STATUS_SEVERITY, Approval, ApprovalStatus } from './approvals.model';
import { ApprovalsStore } from './approvals.store';

type StatusFilter = ApprovalStatus | 'ALL';

@Component({
  selector: 'app-approvals-page',
  standalone: true,
  imports: [
    ButtonModule,
    FormsModule,
    SelectModule,
    TableModule,
    TranslatePipe,
    StatusTagComponent,
  ],
  templateUrl: './approvals.page.html',
  styleUrl: './approvals.page.css',
})
export class ApprovalsPage {
  private readonly store = inject(ApprovalsStore);
  private readonly auth = inject(AuthService);
  private readonly notify = inject(NotificationService);

  protected readonly approvals = this.store.approvals;
  protected readonly severity = APPROVAL_STATUS_SEVERITY;

  protected readonly statusOptions: StatusFilter[] = ['ALL', 'PENDING', 'APPROVED', 'REJECTED'];
  protected readonly statusFilter = signal<StatusFilter>('PENDING');

  protected readonly decidingId = signal<string | null>(null);

  constructor() {
    this.reload();
  }

  onStatusFilterChange(status: StatusFilter): void {
    this.statusFilter.set(status);
    this.reload();
  }

  /**
   * Decide/reject visible only if: PENDING, caller isn't the requester, and caller's own
   * seniorityLevel is strictly greater than the requester's resolved role seniority.
   * banking_backend enforces this rule too (403 if bypassed) — this is UX-only, not the real
   * gate; see docs/services/banking_backend.md for why the backend check is authoritative.
   */
  canDecide(approval: Approval): boolean {
    if (approval.status !== 'PENDING') return false;
    if (approval.requestedBy === this.auth.employeeId()) return false;
    if (approval.requestedBySeniorityLevel === null) return false;
    return this.auth.seniorityLevel() > approval.requestedBySeniorityLevel;
  }

  async decide(approval: Approval, decision: 'APPROVED' | 'REJECTED'): Promise<void> {
    this.decidingId.set(approval.id);
    try {
      await this.store.decide(approval.id, decision);
      this.notify.success('toast.saved');
    } catch (error) {
      this.notify.error(this.resolveErrorKey(error));
    } finally {
      this.decidingId.set(null);
    }
  }

  private async reload(): Promise<void> {
    const status = this.statusFilter();
    await this.store.load(status === 'ALL' ? null : status);
  }

  private resolveErrorKey(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 403) {
      return 'approvals.error.forbidden';
    }
    return 'approvals.error.generic';
  }
}
