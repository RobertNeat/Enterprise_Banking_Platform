import { Component, computed, inject, signal, viewChild } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { TranslationService } from '../../core/services/translation.service';
import { NotificationService } from '../../core/services/notification.service';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { AuthService } from '../../core/services/auth.service';
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { WizardDialogComponent, WizardStep } from '../../shared/components/wizard-dialog.component';
import { LoansStore } from './loans.store';
import { LoanDetail } from './loans.model';
import { LOAN_PRODUCTS, LOAN_STAGE_SEVERITY, LoanProduct, LoanSegment } from './loans.model';

interface CustomerOption {
  id: string;
  fullName: string;
  segment: string;
}

@Component({
  selector: 'app-loans-page',
  standalone: true,
  imports: [
    DecimalPipe,
    FormsModule,
    ReactiveFormsModule,
    TableModule,
    ButtonModule,
    SelectModule,
    IconFieldModule,
    InputIconModule,
    InputTextModule,
    TranslatePipe,
    StatusTagComponent,
    WizardDialogComponent,
  ],
  templateUrl: './loans.page.html',
})
export class LoansPage {
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly store = inject(LoansStore);
  private readonly notify = inject(NotificationService);
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(ApiBaseUrlService);
  private readonly auth = inject(AuthService);
  protected readonly i18n = inject(TranslationService);

  protected readonly applications = this.store.applications;
  /**
   * Admin can never be a maker-checker requester (backend 403s — see RoleCode.hasHigherSeniorityRole()
   * and ApprovalsService.requireCheckerExists()). submit-for-approval is the wizard's only
   * mutating step after loan creation, so the whole "new application" wizard is gated for admin
   * rather than letting them create an orphan application stuck at stage 'application'.
   */
  protected readonly canCreateLoan = this.auth.canInitiateMakerChecker;
  protected readonly stageSeverity = LOAN_STAGE_SEVERITY;
  protected readonly products = LOAN_PRODUCTS;

  protected readonly searchTerm = signal('');
  protected readonly segmentFilter = signal<LoanSegment | 'all'>('all');

  protected readonly customers = signal<CustomerOption[]>([]);

  constructor() {
    this.http
      .get<CustomerOption[]>(`${this.apiBaseUrl.bankingBackend}/customers`)
      .subscribe((list) => this.customers.set(list));
  }

  protected readonly segmentOptions = computed(() => [
    { label: this.i18n.translate('common.all'), value: 'all' as const },
    { label: this.i18n.translate('loans.segment.retail'), value: 'retail' as const },
    { label: this.i18n.translate('loans.segment.business'), value: 'business' as const },
  ]);

  protected readonly wizardSegmentOptions = computed(() => [
    { label: this.i18n.translate('loans.segment.retail'), value: 'retail' as const },
    { label: this.i18n.translate('loans.segment.business'), value: 'business' as const },
  ]);

  protected readonly productOptions = computed(() =>
    this.products.map((product) => ({
      label: this.i18n.translate('loans.product.' + product),
      value: product,
    })),
  );

  protected readonly customerOptions = computed(() =>
    this.customers().map((c) => ({ label: `${c.fullName} (${c.id})`, value: c.id })),
  );

  protected readonly filtered = computed(() => {
    const term = this.searchTerm().toLowerCase();
    const segment = this.segmentFilter();
    return this.applications().filter((a) => {
      const matchesSegment = segment === 'all' || a.segment === segment;
      const matchesTerm =
        !term || a.applicant.toLowerCase().includes(term) || a.id.toLowerCase().includes(term);
      return matchesSegment && matchesTerm;
    });
  });

  openDetail(id: string): void {
    this.router.navigate(['/loans', id]);
  }

  // --- Wizard: nowy wniosek kredytowy ---
  //
  // Krok 2 (scoring) nie jest już formularzem — po wypełnieniu kroków 0-1 wizard wywołuje
  // POST /loans (scoring w credit_risk_mock) i wyświetla wynik jako read-only podsumowanie.
  // Submit na kroku 3 woła POST /loans/{id}/submit-for-approval (otwiera kolejkę zatwierdzeń).
  // Wynik scoringu nie jest już polem formularza (patrz docs/services/banking_backend.md, F2).

  protected readonly wizard = viewChild(WizardDialogComponent);
  protected readonly wizardOpen = signal(false);
  protected readonly wizardStep = signal(0);
  protected readonly scoring = signal(false);
  protected readonly scoringError = signal(false);
  protected readonly createdLoan = signal<LoanDetail | null>(null);

  protected readonly wizardSteps: WizardStep[] = [
    { labelKey: 'loans.wizard.step1' },
    { labelKey: 'loans.wizard.step2' },
    { labelKey: 'loans.wizard.step3' },
    { labelKey: 'loans.wizard.step4' },
  ];

  protected readonly form = this.fb.nonNullable.group({
    customerId: this.fb.nonNullable.control<string | null>(null, Validators.required),
    segment: this.fb.nonNullable.control<LoanSegment | null>(null, Validators.required),
    product: this.fb.nonNullable.control<LoanProduct | null>(null, Validators.required),
    requestedAmount: this.fb.nonNullable.control<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
    ]),
  });

  private readonly formTick = toSignal(this.form.valueChanges, { initialValue: null });

  protected readonly stepValid = computed(() => {
    this.formTick();
    const step = this.wizardStep();
    const f = this.form.controls;
    switch (step) {
      case 0:
        return f.customerId.valid && f.segment.valid;
      case 1:
        return f.product.valid && f.requestedAmount.valid;
      case 2:
        return !this.scoring() && !this.scoringError() && this.createdLoan() !== null;
      case 3:
        return this.createdLoan() !== null;
      default:
        return false;
    }
  });

  openWizard(): void {
    this.form.reset();
    this.wizardStep.set(0);
    this.createdLoan.set(null);
    this.scoringError.set(false);
    this.wizard()?.reset();
    this.wizardOpen.set(true);
  }

  async onWizardStepChange(step: number): Promise<void> {
    this.wizardStep.set(step);
    if (step === 2 && this.createdLoan() === null) {
      await this.scoreApplication();
    }
  }

  closeWizard(): void {
    this.wizardOpen.set(false);
    this.wizardStep.set(0);
    this.createdLoan.set(null);
    this.form.reset();
  }

  private async scoreApplication(): Promise<void> {
    const value = this.form.getRawValue();
    this.scoring.set(true);
    this.scoringError.set(false);
    try {
      const loan = await this.store.create({
        customerId: value.customerId!,
        segment: value.segment!,
        product: value.product!,
        requestedAmount: value.requestedAmount!,
      });
      this.createdLoan.set(loan);
    } catch {
      this.scoringError.set(true);
      this.notify.error('loans.error.generic');
    } finally {
      this.scoring.set(false);
    }
  }

  async submitWizard(): Promise<void> {
    const loan = this.createdLoan();
    if (!loan) {
      return;
    }
    try {
      await this.store.submitForApproval(loan.id);
      this.notify.success('toast.saved');
    } catch (error) {
      this.notify.error(this.resolveErrorKey(error));
    } finally {
      this.wizardOpen.set(false);
      this.wizardStep.set(0);
      this.createdLoan.set(null);
      this.form.reset();
    }
  }

  private resolveErrorKey(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 403) {
      return 'loans.error.forbidden';
    }
    return 'loans.error.generic';
  }
}
