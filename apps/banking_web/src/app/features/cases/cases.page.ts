import { Component, computed, inject, signal, viewChild } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { TranslationService } from '../../core/services/translation.service';
import { NotificationService } from '../../core/services/notification.service';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { WizardDialogComponent, WizardStep } from '../../shared/components/wizard-dialog.component';
import { AuditHistoryDialogComponent } from '../../shared/audit-history/audit-history-dialog.component';
import { AuditHistoryStore } from '../../shared/audit-history/audit-history.store';
import { PRIORITY_SEVERITY, Priority } from '../../shared/utils/status-severity';
import { CasesStore } from './cases.store';
import { CASE_SEVERITY, CustomerCase } from './cases.model';

interface CustomerOption {
  id: string;
  fullName: string;
}

const CASE_TYPES = ['complaint', 'restructuring_request', 'inquiry'] as const;
type CaseType = (typeof CASE_TYPES)[number];

@Component({
  selector: 'app-cases-page',
  standalone: true,
  imports: [
    ButtonModule,
    TableModule,
    TranslatePipe,
    StatusTagComponent,
    ReactiveFormsModule,
    InputTextModule,
    SelectModule,
    WizardDialogComponent,
    AuditHistoryDialogComponent,
  ],
  templateUrl: './cases.page.html',
  styleUrl: './cases.page.css',
})
export class CasesPage {
  private readonly notify = inject(NotificationService);
  private readonly fb = inject(FormBuilder);
  private readonly store = inject(CasesStore);
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(ApiBaseUrlService);
  protected readonly i18n = inject(TranslationService);
  protected readonly historyStore = inject(AuditHistoryStore);

  protected readonly cases = this.store.cases;
  protected readonly severity = CASE_SEVERITY;
  protected readonly prioritySeverity = PRIORITY_SEVERITY;

  protected readonly priorityOptions: Priority[] = ['low', 'medium', 'high'];
  protected readonly caseTypeOptions = computed(() =>
    CASE_TYPES.map((type) => ({ label: this.i18n.translate('cases.type.' + type), value: type })),
  );

  protected readonly customers = signal<CustomerOption[]>([]);
  protected readonly customerOptions = computed(() =>
    this.customers().map((c) => ({ label: `${c.fullName} (${c.id})`, value: c.id })),
  );

  constructor() {
    this.http
      .get<CustomerOption[]>(`${this.apiBaseUrl.bankingBackend}/customers`)
      .subscribe((list) => this.customers.set(list));
  }

  protected readonly wizardVisible = signal(false);
  protected readonly wizardStep = signal(0);
  protected readonly wizardRef = viewChild(WizardDialogComponent);
  protected readonly submitting = signal(false);

  protected readonly wizardSteps: WizardStep[] = [
    { labelKey: 'cases.wizard.step1' },
    { labelKey: 'cases.wizard.step2' },
    { labelKey: 'cases.wizard.step3' },
  ];

  protected readonly form = this.fb.group({
    details: this.fb.group({
      subject: this.fb.control('', { validators: [Validators.required] }),
      customerId: this.fb.control<string | null>(null, { validators: [Validators.required] }),
      caseType: this.fb.control<CaseType | null>(null, { validators: [Validators.required] }),
    }),
    schedule: this.fb.group({
      priority: this.fb.control<Priority | null>(null, { validators: [Validators.required] }),
    }),
  });

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
      customerId: details.customerId ?? '',
      caseType: details.caseType,
      priority: schedule.priority,
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

  async submitWizard(): Promise<void> {
    if (this.form.invalid) {
      return;
    }
    const details = this.form.controls.details.getRawValue();
    const schedule = this.form.controls.schedule.getRawValue();
    this.submitting.set(true);
    try {
      // statutoryDeadline/slaRemainingHours are computed by banking_backend (F8), not sent here.
      await this.store.create({
        subject: details.subject!,
        customerId: details.customerId!,
        priority: schedule.priority!,
        caseType: details.caseType!,
      });
      this.notify.success('toast.saved');
      this.wizardVisible.set(false);
      this.wizardStep.set(0);
      this.form.reset();
    } catch {
      this.notify.error('cases.error.generic');
    } finally {
      this.submitting.set(false);
    }
  }

  async escalate(item: CustomerCase): Promise<void> {
    if (item.status === 'resolved' || item.status === 'escalated') {
      this.notify.warn('toast.actionBlocked');
      return;
    }
    if (item.slaRemainingHours <= 24) {
      this.notify.warn('toast.deadlineClose');
    }
    try {
      await this.store.escalate(item.id);
      this.notify.success('toast.saved');
    } catch {
      this.notify.error('cases.error.generic');
    }
  }

  /** CAS lifecycle — only 'new' may start; backend returns 409 otherwise. */
  async start(item: CustomerCase): Promise<void> {
    if (item.status !== 'new') {
      this.notify.warn('toast.actionBlocked');
      return;
    }
    try {
      await this.store.start(item.id);
      this.notify.success('toast.saved');
    } catch (error) {
      this.handleActionError(error);
    }
  }

  /** CAS lifecycle — 'new'/'inProgress'/'escalated' may resolve; backend returns 409 if already resolved. */
  async resolve(item: CustomerCase): Promise<void> {
    if (item.status === 'resolved') {
      this.notify.warn('toast.actionBlocked');
      return;
    }
    try {
      await this.store.resolve(item.id);
      this.notify.success('toast.saved');
    } catch (error) {
      this.handleActionError(error);
    }
  }

  private handleActionError(error: unknown): void {
    if (error instanceof HttpErrorResponse && error.status === 409) {
      this.notify.warn('toast.actionBlocked');
      return;
    }
    this.notify.error('cases.error.generic');
  }

  // --- Ślad audytowy ("Historia") ---

  protected readonly historyDialogOpen = signal(false);

  openHistoryDialog(item: CustomerCase): void {
    this.historyDialogOpen.set(true);
    void this.historyStore.load('CUSTOMER_CASE', item.id);
  }

  closeHistoryDialog(): void {
    this.historyDialogOpen.set(false);
    this.historyStore.clear();
  }
}
