import { Component, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { StepperModule } from 'primeng/stepper';
import { TableModule } from 'primeng/table';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { NotificationService } from '../../core/services/notification.service';
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { AuditHistoryDialogComponent } from '../../shared/audit-history/audit-history-dialog.component';
import { AuditHistoryStore } from '../../shared/audit-history/audit-history.store';
import { buildSchedule } from './loans.data';
import {
  INSTALLMENT_SEVERITY,
  Installment,
  LOAN_STAGES,
  LOAN_STAGE_SEVERITY,
  LoanApplication,
  LoanDetail,
  LoanStageTransitionEntry,
} from './loans.model';
import { LoansStore } from './loans.store';

const EMPTY_LOAN: LoanApplication = {
  id: '',
  applicant: '',
  segment: 'retail',
  product: 'cash',
  stage: 'application',
  requestedAmount: 0,
  approvedAmount: null,
  scoreResult: 0,
  dsti: 0,
  createdAt: '',
};

@Component({
  selector: 'app-loan-detail-page',
  standalone: true,
  imports: [
    DecimalPipe,
    ButtonModule,
    StepperModule,
    TableModule,
    TranslatePipe,
    StatusTagComponent,
    AuditHistoryDialogComponent,
  ],
  templateUrl: './loan-detail.page.html',
})
export class LoanDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly notify = inject(NotificationService);
  private readonly store = inject(LoansStore);
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(ApiBaseUrlService);
  protected readonly historyStore = inject(AuditHistoryStore);

  protected readonly stages = LOAN_STAGES;
  protected readonly stageSeverity = LOAN_STAGE_SEVERITY;
  protected readonly installmentSeverity = INSTALLMENT_SEVERITY;

  private readonly fetchedLoan = signal<LoanDetail | null>(null);
  private readonly fetchedInstallments = signal<Installment[] | null>(null);
  private readonly fetchedTransitions = signal<LoanStageTransitionEntry[]>([]);

  /**
   * loan() może zwrócić LoanApplication z LoansStore (bez stageTransitions), więc oś przejść
   * etapów ma własny signal zasilany tym samym GET /loans/{id} i POST /advance-stage, które
   * zwracają pełny LoanDetail.
   */
  protected readonly stageTransitions = computed(() =>
    [...this.fetchedTransitions()].sort((a, b) => a.transitionedAt.localeCompare(b.transitionedAt)),
  );

  protected readonly loan = computed(() => {
    const id = this.route.snapshot.paramMap.get('id') ?? '';
    return this.store.findById(id) ?? this.fetchedLoan() ?? EMPTY_LOAN;
  });

  protected readonly stageIndex = computed(() => this.stages.indexOf(this.loan().stage));

  protected readonly schedule = computed(() => {
    const fetched = this.fetchedInstallments();
    if (fetched) {
      // Backend answered — even an empty array is authoritative (e.g. a loan still in
      // 'application' stage genuinely has no schedule yet). Only fall back to a locally
      // generated schedule before the backend has responded at all (see constructor).
      return fetched;
    }
    const amount = this.loan().approvedAmount ?? this.loan().requestedAmount;
    if (amount <= 0) {
      return [];
    }
    // fetchedLoan carries the full LoanDetail (interestRate/termMonths) when the backend has
    // answered; EMPTY_LOAN / a bare LoanApplication from the store has neither, so fall back
    // to the defaults previously hardcoded here.
    const detail = this.fetchedLoan();
    const termMonths = detail?.termMonths ?? 12;
    const annualRate = detail?.interestRate ?? 0.079;
    return buildSchedule(amount, termMonths, annualRate);
  });

  constructor() {
    const id = this.route.snapshot.paramMap.get('id') ?? '';
    // Always hit the detail endpoint — LoansStore's list only carries the LoanApplication
    // fields, never installments, so this is the only way to get the real schedule for
    // LN-80231/LN-80203 (the loans that actually have seeded installments). A deep link
    // (direct navigation, loan not yet in the store) is handled the same way: the fetched
    // loan becomes the fallback the `loan` computed uses once the store doesn't have it.
    if (id) {
      this.http.get<LoanDetail>(`${this.apiBaseUrl.bankingBackend}/loans/${id}`).subscribe({
        next: (detail) => {
          this.fetchedLoan.set(detail);
          this.fetchedInstallments.set(detail.installments);
          this.fetchedTransitions.set(detail.stageTransitions ?? []);
        },
        error: () => {
          // Unknown id — EMPTY_LOAN / locally-generated schedule fallback keeps the
          // template rendering without throwing (e.g. a wizard-created loan with no
          // backend row yet).
        },
      });
    }
  }

  back(): void {
    this.router.navigate(['/loans']);
  }

  protected readonly canAdvance = computed(() => this.loan().stage === 'decision');
  /** application stage covers both a brand-new loan and one bounced back from a REJECTED decision. */
  protected readonly canSubmitForApproval = computed(() => this.loan().stage === 'application');
  protected readonly canDisburse = computed(() => this.loan().stage === 'contract');

  /**
   * decision -> contract requires the linked governance_mock approval to already be APPROVED
   * (see LoanActionsService.advanceStage) — the employee clicks this after seeing the approval
   * decided in the approvals queue (R2 design: manual action, no automatic callback from
   * ApprovalsService.decide(), see docs/services/banking_backend.md). A REJECTED decision instead
   * comes back with stage 'application' (200, not an error) so the cycle can be closed by
   * resubmitting rather than getting stuck.
   */
  async advanceStage(): Promise<void> {
    if (!this.canAdvance()) {
      this.notify.warn('toast.actionBlocked');
      return;
    }
    try {
      const updated = await this.store.advanceStage(this.loan().id);
      this.fetchedLoan.set(updated);
      this.fetchedInstallments.set(updated.installments);
      this.fetchedTransitions.set(updated.stageTransitions ?? []);
      if (updated.stage === 'application') {
        this.notify.warn('loans.rejected');
      } else {
        this.notify.success('toast.saved');
      }
    } catch {
      this.notify.error('loans.error.generic');
    }
  }

  /** application -> decision: reopens a governance_mock approval, used for both first submission and resubmission after a rejection. */
  async submitForApproval(): Promise<void> {
    if (!this.canSubmitForApproval()) {
      this.notify.warn('toast.actionBlocked');
      return;
    }
    try {
      const response = await this.store.submitForApproval(this.loan().id);
      this.fetchedLoan.set(response.loan);
      this.fetchedInstallments.set(response.loan.installments);
      this.fetchedTransitions.set(response.loan.stageTransitions ?? []);
      this.notify.success('toast.saved');
    } catch {
      this.notify.error('loans.error.generic');
    }
  }

  /** contract -> disbursement -> schedule: operational payout step after the contract stage. */
  async disburse(): Promise<void> {
    if (!this.canDisburse()) {
      this.notify.warn('toast.actionBlocked');
      return;
    }
    try {
      const updated = await this.store.disburse(this.loan().id);
      this.fetchedLoan.set(updated);
      this.fetchedInstallments.set(updated.installments);
      this.fetchedTransitions.set(updated.stageTransitions ?? []);
      this.notify.success('toast.saved');
    } catch {
      this.notify.error('loans.error.generic');
    }
  }

  // --- Ślad audytowy ("Historia") ---

  protected readonly historyDialogOpen = signal(false);

  openHistoryDialog(): void {
    this.historyDialogOpen.set(true);
    void this.historyStore.load('LOAN_APPLICATION', this.loan().id);
  }

  closeHistoryDialog(): void {
    this.historyDialogOpen.set(false);
    this.historyStore.clear();
  }
}
