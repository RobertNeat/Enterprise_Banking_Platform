import { Component, inject, signal, viewChild } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { NotificationService } from '../../core/services/notification.service';
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { WizardDialogComponent, WizardStep } from '../../shared/components/wizard-dialog.component';
import { ENFORCEMENT_CASES } from './enforcement.data';
import { ENFORCEMENT_SEVERITY, EnforcementCase } from './enforcement.model';

/** Domyślna kwota wolna od zajęcia (PLN) — zgodna z istniejącymi danymi demo. */
const DEFAULT_PROTECTED_AMOUNT = 1631.4;

@Component({
  selector: 'app-enforcement-page',
  standalone: true,
  imports: [
    DecimalPipe,
    ReactiveFormsModule,
    ButtonModule,
    TableModule,
    InputTextModule,
    InputNumberModule,
    TranslatePipe,
    StatusTagComponent,
    WizardDialogComponent,
  ],
  templateUrl: './enforcement.page.html',
})
export class EnforcementPage {
  private readonly notify = inject(NotificationService);
  private readonly fb = inject(FormBuilder);

  private readonly wizard = viewChild.required(WizardDialogComponent);

  protected readonly cases = signal<EnforcementCase[]>(ENFORCEMENT_CASES);
  protected readonly severity = ENFORCEMENT_SEVERITY;

  protected readonly wizardVisible = signal(false);
  protected readonly wizardStep = signal(0);

  protected readonly wizardSteps: WizardStep[] = [
    { labelKey: 'enforcement.wizard.step1' },
    { labelKey: 'enforcement.wizard.step2' },
    { labelKey: 'enforcement.wizard.step3' },
  ];

  protected readonly form = this.fb.nonNullable.group({
    details: this.fb.nonNullable.group({
      customer: this.fb.nonNullable.control('', [Validators.required]),
      bailiff: this.fb.nonNullable.control('', [Validators.required]),
      caseNumber: this.fb.nonNullable.control('', [Validators.required, Validators.minLength(5)]),
    }),
    amounts: this.fb.nonNullable.group({
      blockedAmount: this.fb.nonNullable.control(0, [Validators.required, Validators.min(0)]),
      transferredAmount: this.fb.nonNullable.control(0, [Validators.required, Validators.min(0)]),
      protectedAmount: this.fb.nonNullable.control(DEFAULT_PROTECTED_AMOUNT, [
        Validators.required,
        Validators.min(0),
      ]),
    }),
  });

  protected get detailsGroup() {
    return this.form.controls.details;
  }

  protected get amountsGroup() {
    return this.form.controls.amounts;
  }

  openWizard(): void {
    this.form.reset({
      details: { customer: '', bailiff: '', caseNumber: '' },
      amounts: {
        blockedAmount: 0,
        transferredAmount: 0,
        protectedAmount: DEFAULT_PROTECTED_AMOUNT,
      },
    });
    this.wizardStep.set(0);
    this.wizard().reset();
    this.wizardVisible.set(true);
  }

  onWizardVisibleChange(visible: boolean): void {
    this.wizardVisible.set(visible);
    if (!visible) {
      this.wizardStep.set(0);
    }
  }

  onWizardStepChange(step: number): void {
    this.wizardStep.set(step);
  }

  isStepValid(): boolean {
    switch (this.wizardStep()) {
      case 0:
        return this.detailsGroup.valid;
      case 1:
        return this.amountsGroup.valid;
      default:
        return this.form.valid;
    }
  }

  cancelWizard(): void {
    this.wizardVisible.set(false);
    this.wizardStep.set(0);
    this.form.reset({
      details: { customer: '', bailiff: '', caseNumber: '' },
      amounts: {
        blockedAmount: 0,
        transferredAmount: 0,
        protectedAmount: DEFAULT_PROTECTED_AMOUNT,
      },
    });
  }

  submitWizard(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { details, amounts } = this.form.getRawValue();
    const newCase: EnforcementCase = {
      id: this.generateCaseId(),
      customer: details.customer,
      bailiff: details.bailiff,
      caseNumber: details.caseNumber,
      blockedAmount: amounts.blockedAmount,
      transferredAmount: amounts.transferredAmount,
      protectedAmount: amounts.protectedAmount,
      status: 'registered',
    };
    this.cases.update((list) => [newCase, ...list]);
    this.wizardVisible.set(false);
    this.wizardStep.set(0);
    this.form.reset({
      details: { customer: '', bailiff: '', caseNumber: '' },
      amounts: {
        blockedAmount: 0,
        transferredAmount: 0,
        protectedAmount: DEFAULT_PROTECTED_AMOUNT,
      },
    });
    this.notify.success('toast.saved');
  }

  transfer(item: EnforcementCase): void {
    if (item.status !== 'active') {
      this.notify.warn('toast.actionBlocked');
      return;
    }
    this.notify.success('toast.saved');
  }

  private generateCaseId(): string {
    const existingIds = new Set(this.cases().map((c) => c.id));
    let id: string;
    do {
      const digits = Math.floor(1000 + Math.random() * 9000);
      id = `EN-${digits}`;
    } while (existingIds.has(id));
    return id;
  }
}
