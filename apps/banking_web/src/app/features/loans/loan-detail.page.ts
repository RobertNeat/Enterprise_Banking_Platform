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
import { buildSchedule } from './loans.data';
import {
  INSTALLMENT_SEVERITY,
  Installment,
  LOAN_STAGES,
  LOAN_STAGE_SEVERITY,
  LoanApplication,
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

  protected readonly stages = LOAN_STAGES;
  protected readonly stageSeverity = LOAN_STAGE_SEVERITY;
  protected readonly installmentSeverity = INSTALLMENT_SEVERITY;

  private readonly fetchedLoan = signal<LoanApplication | null>(null);
  private readonly fetchedInstallments = signal<Installment[] | null>(null);

  protected readonly loan = computed(() => {
    const id = this.route.snapshot.paramMap.get('id') ?? '';
    return this.store.findById(id) ?? this.fetchedLoan() ?? EMPTY_LOAN;
  });

  protected readonly stageIndex = computed(() => this.stages.indexOf(this.loan().stage));

  protected readonly schedule = computed(() => {
    const fetched = this.fetchedInstallments();
    if (fetched && fetched.length > 0) {
      return fetched;
    }
    const amount = this.loan().approvedAmount ?? this.loan().requestedAmount;
    return amount > 0 ? buildSchedule(amount, 12) : [];
  });

  constructor() {
    const id = this.route.snapshot.paramMap.get('id') ?? '';
    // Always hit the detail endpoint — LoansStore's list only carries the LoanApplication
    // fields, never installments, so this is the only way to get the real schedule for
    // LN-80231/LN-80203 (the loans that actually have seeded installments). A deep link
    // (direct navigation, loan not yet in the store) is handled the same way: the fetched
    // loan becomes the fallback the `loan` computed uses once the store doesn't have it.
    if (id) {
      this.http
        .get<LoanApplication & { installments: Installment[] }>(
          `${this.apiBaseUrl.bankingBackend}/loans/${id}`,
        )
        .subscribe({
          next: (detail) => {
            this.fetchedLoan.set(detail);
            this.fetchedInstallments.set(detail.installments);
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

  advanceStage(): void {
    const idx = this.stageIndex();
    if (idx >= this.stages.length - 1) {
      this.notify.warn('toast.actionBlocked');
      return;
    }
    this.notify.success('toast.saved');
  }
}
