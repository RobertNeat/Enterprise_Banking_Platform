import { Component, computed, inject, signal, viewChild } from '@angular/core';
import { DecimalPipe } from '@angular/common';
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
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { WizardDialogComponent, WizardStep } from '../../shared/components/wizard-dialog.component';
import { LoansStore } from './loans.store';
import { LOAN_PRODUCTS, LOAN_STAGE_SEVERITY, LoanProduct, LoanSegment } from './loans.model';

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
  protected readonly i18n = inject(TranslationService);

  protected readonly applications = this.store.applications;
  protected readonly stageSeverity = LOAN_STAGE_SEVERITY;
  protected readonly products = LOAN_PRODUCTS;

  protected readonly searchTerm = signal('');
  protected readonly segmentFilter = signal<LoanSegment | 'all'>('all');

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

  protected readonly wizard = viewChild(WizardDialogComponent);
  protected readonly wizardOpen = signal(false);
  protected readonly wizardStep = signal(0);

  protected readonly wizardSteps: WizardStep[] = [
    { labelKey: 'loans.wizard.step1' },
    { labelKey: 'loans.wizard.step2' },
    { labelKey: 'loans.wizard.step3' },
    { labelKey: 'loans.wizard.step4' },
  ];

  protected readonly form = this.fb.nonNullable.group({
    applicant: ['', [Validators.required, Validators.minLength(3)]],
    segment: this.fb.nonNullable.control<LoanSegment | null>(null, Validators.required),
    product: this.fb.nonNullable.control<LoanProduct | null>(null, Validators.required),
    requestedAmount: this.fb.nonNullable.control<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
    ]),
    scoreResult: this.fb.nonNullable.control<number | null>(null, [
      Validators.required,
      Validators.min(0),
      Validators.max(1000),
    ]),
    dsti: this.fb.nonNullable.control<number | null>(null, [
      Validators.required,
      Validators.min(0),
      Validators.max(1),
    ]),
  });

  private readonly formTick = toSignal(this.form.valueChanges, { initialValue: null });

  protected readonly stepValid = computed(() => {
    this.formTick();
    const step = this.wizardStep();
    const f = this.form.controls;
    switch (step) {
      case 0:
        return f.applicant.valid && f.segment.valid;
      case 1:
        return f.product.valid && f.requestedAmount.valid;
      case 2:
        return f.scoreResult.valid && f.dsti.valid;
      case 3:
        return this.form.valid;
      default:
        return false;
    }
  });

  openWizard(): void {
    this.form.reset();
    this.wizardStep.set(0);
    this.wizard()?.reset();
    this.wizardOpen.set(true);
  }

  onWizardStepChange(step: number): void {
    this.wizardStep.set(step);
  }

  closeWizard(): void {
    this.wizardOpen.set(false);
    this.wizardStep.set(0);
    this.form.reset();
  }

  submitWizard(): void {
    if (this.form.invalid) {
      return;
    }
    const value = this.form.getRawValue();
    const id = this.generateId();
    this.store.add({
      id,
      applicant: value.applicant,
      segment: value.segment!,
      product: value.product!,
      stage: 'application',
      requestedAmount: value.requestedAmount!,
      approvedAmount: null,
      scoreResult: value.scoreResult!,
      dsti: value.dsti!,
      createdAt: new Date().toISOString().slice(0, 10),
    });
    this.wizardOpen.set(false);
    this.wizardStep.set(0);
    this.form.reset();
    this.notify.success('toast.saved');
  }

  private generateId(): string {
    let id: string;
    do {
      const digits = Math.floor(10000 + Math.random() * 90000);
      id = `LN-${digits}`;
    } while (this.store.findById(id));
    return id;
  }
}
