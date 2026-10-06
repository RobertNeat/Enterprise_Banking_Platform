import { Component, computed, inject, signal, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { NotificationService } from '../../core/services/notification.service';
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { WizardDialogComponent, WizardStep } from '../../shared/components/wizard-dialog.component';
import { PRIORITY_SEVERITY, Priority } from '../../shared/utils/status-severity';
import { CUSTOMER_CASES } from './cases.data';
import { CASE_SEVERITY, CustomerCase } from './cases.model';

/** Formatuje datę lokalnie jako YYYY-MM-DD (bez konwersji do UTC, patrz toISOString). */
function formatDateLocal(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function generateCaseId(existing: readonly CustomerCase[]): string {
  const existingIds = new Set(existing.map((c) => c.id));
  let id: string;
  do {
    const n = Math.floor(10000 + Math.random() * 90000);
    id = `CS-${n}`;
  } while (existingIds.has(id));
  return id;
}

@Component({
  selector: 'app-cases-page',
  standalone: true,
  imports: [
    ButtonModule,
    TableModule,
    TranslatePipe,
    StatusTagComponent,
    ReactiveFormsModule,
    DatePickerModule,
    InputNumberModule,
    InputTextModule,
    SelectModule,
    WizardDialogComponent,
  ],
  templateUrl: './cases.page.html',
  styleUrl: './cases.page.css',
})
export class CasesPage {
  private readonly notify = inject(NotificationService);
  private readonly fb = inject(FormBuilder);

  protected readonly cases = signal<CustomerCase[]>(CUSTOMER_CASES);
  protected readonly severity = CASE_SEVERITY;
  protected readonly prioritySeverity = PRIORITY_SEVERITY;

  protected readonly priorityOptions: Priority[] = ['low', 'medium', 'high'];

  protected readonly wizardVisible = signal(false);
  protected readonly wizardStep = signal(0);
  protected readonly wizardRef = viewChild(WizardDialogComponent);

  protected readonly wizardSteps: WizardStep[] = [
    { labelKey: 'cases.wizard.step1' },
    { labelKey: 'cases.wizard.step2' },
    { labelKey: 'cases.wizard.step3' },
  ];

  protected readonly form = this.fb.group({
    details: this.fb.group({
      subject: this.fb.control('', { validators: [Validators.required] }),
      customer: this.fb.control('', { validators: [Validators.required] }),
      owner: this.fb.control('', { validators: [Validators.required] }),
    }),
    schedule: this.fb.group({
      priority: this.fb.control<Priority | null>(null, { validators: [Validators.required] }),
      statutoryDeadline: this.fb.control<Date | null>(null, { validators: [Validators.required] }),
      slaRemainingHours: this.fb.control<number | null>(null, {
        validators: [Validators.required, Validators.min(0)],
      }),
    }),
  });

  // `valueChanges` (nie `statusChanges`!) — status formularza (VALID/INVALID)
  // nie zmienia się przy każdej zmianie wartości (np. wypełnienie kroku 1
  // podczas gdy krok 2 jest jeszcze invalid: root form zostaje INVALID w
  // obu przypadkach), więc `toSignal(statusChanges)` z `Object.is` nie
  // wykrywa zmiany i computed() nigdy się nie przelicza — Next zostaje
  // trwale wyszarzony mimo poprawnych danych. `valueChanges` emituje nowy
  // obiekt przy każdej zmianie wartości, więc ten problem nie występuje.
  private readonly formTick = toSignal(this.form.valueChanges, { initialValue: null });

  protected readonly stepValid = computed(() => {
    this.formTick();
    switch (this.wizardStep()) {
      case 0:
        return this.form.controls.details.valid;
      case 1:
        return this.form.controls.schedule.valid;
      default:
        return this.form.valid;
    }
  });

  protected readonly summary = computed(() => {
    this.formTick();
    const details = this.form.controls.details.getRawValue();
    const schedule = this.form.controls.schedule.getRawValue();
    return {
      subject: details.subject ?? '',
      customer: details.customer ?? '',
      owner: details.owner ?? '',
      priority: schedule.priority,
      statutoryDeadline: schedule.statutoryDeadline
        ? formatDateLocal(schedule.statutoryDeadline)
        : '',
      slaRemainingHours: schedule.slaRemainingHours,
    };
  });

  openWizard(): void {
    this.form.reset();
    this.wizardStep.set(0);
    this.wizardRef()?.reset();
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

  cancelWizard(): void {
    this.wizardVisible.set(false);
    this.wizardStep.set(0);
    this.form.reset();
  }

  submitWizard(): void {
    if (this.form.invalid) {
      return;
    }
    const details = this.form.controls.details.getRawValue();
    const schedule = this.form.controls.schedule.getRawValue();
    const newCase: CustomerCase = {
      id: generateCaseId(this.cases()),
      subject: details.subject!,
      customer: details.customer!,
      owner: details.owner!,
      priority: schedule.priority!,
      statutoryDeadline: formatDateLocal(schedule.statutoryDeadline!),
      slaRemainingHours: schedule.slaRemainingHours!,
      status: 'new',
    };
    this.cases.update((list) => [newCase, ...list]);
    this.wizardVisible.set(false);
    this.wizardStep.set(0);
    this.form.reset();
    this.notify.success('toast.saved');
  }

  escalate(item: CustomerCase): void {
    if (item.status === 'resolved') {
      this.notify.warn('toast.actionBlocked');
      return;
    }
    if (item.slaRemainingHours <= 24) {
      this.notify.warn('toast.deadlineClose');
    }
    this.cases.update((list) =>
      list.map((c) => (c.id === item.id ? { ...c, status: 'escalated' } : c)),
    );
    this.notify.success('toast.saved');
  }
}
