import { Component, computed, inject, signal, viewChild } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { SelectModule } from 'primeng/select';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { NotificationService } from '../../core/services/notification.service';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { AuthService } from '../../core/services/auth.service';
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { WizardDialogComponent, WizardStep } from '../../shared/components/wizard-dialog.component';
import { AuditHistoryDialogComponent } from '../../shared/audit-history/audit-history-dialog.component';
import { AuditHistoryStore } from '../../shared/audit-history/audit-history.store';
import { EnforcementStore } from './enforcement.store';
import { ENFORCEMENT_SEVERITY, EnforcementCase, EnforcementKind } from './enforcement.model';

/** Domyślna kwota wolna od zajęcia (PLN) — zgodna z istniejącymi danymi demo. */
const DEFAULT_PROTECTED_AMOUNT = 1631.4;

/**
 * ZJC-03 — limity kwoty wolnej per rodzaj zajęcia, lustrzane wobec
 * EnforcementActionsService (banking.enforcement.protected-amount-limit.*). Tylko do podglądu/
 * ostrzeżenia w kreatorze — backend jest autorytatywny i waliduje ponownie (400 przy przekroczeniu).
 * NON_ALIMONY: 75% minimalnego wynagrodzenia brutto 2026 (4806 PLN, Dz.U. 2025 poz. 1242) = 3604.50 PLN
 * (art. 54 ust. 1 Prawa bankowego). ALIMONY: 0.00 — zajęcie alimentacyjne nie korzysta z tego
 * zwolnienia w tym demo. Wartości orientacyjne, konfigurowalne, nie są wiążącą poradą prawną.
 */
const PROTECTED_AMOUNT_LIMITS: Record<EnforcementKind, number> = {
  NON_ALIMONY: 3604.5,
  ALIMONY: 0,
};

/** Mirrors the backend's 400: protectedAmount must not exceed the selected kind's limit. */
function protectedAmountWithinLimitValidator(group: AbstractControl): ValidationErrors | null {
  const kind = group.get('kind')?.value as EnforcementKind | null;
  const protectedAmount = group.get('protectedAmount')?.value as number | null;
  if (!kind || protectedAmount == null) {
    return null;
  }
  return protectedAmount > PROTECTED_AMOUNT_LIMITS[kind]
    ? { protectedAmountExceedsLimit: true }
    : null;
}

interface CustomerOption {
  id: string;
  fullName: string;
}

@Component({
  selector: 'app-enforcement-page',
  standalone: true,
  imports: [
    DecimalPipe,
    ReactiveFormsModule,
    ButtonModule,
    DialogModule,
    TableModule,
    InputTextModule,
    InputNumberModule,
    SelectModule,
    TranslatePipe,
    StatusTagComponent,
    WizardDialogComponent,
    AuditHistoryDialogComponent,
  ],
  templateUrl: './enforcement.page.html',
})
export class EnforcementPage {
  private readonly notify = inject(NotificationService);
  private readonly fb = inject(FormBuilder);
  private readonly store = inject(EnforcementStore);
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(ApiBaseUrlService);
  private readonly auth = inject(AuthService);
  protected readonly historyStore = inject(AuditHistoryStore);

  private readonly wizard = viewChild.required(WizardDialogComponent);

  protected readonly cases = this.store.cases;
  protected readonly severity = ENFORCEMENT_SEVERITY;
  /**
   * Admin can never be a maker-checker requester (backend 403s — see RoleCode.hasHigherSeniorityRole()
   * and ApprovalsService.requireCheckerExists()). Registration itself has no seniority gate, so
   * only the per-row "request transfer approval" action is hidden, not the whole wizard.
   */
  protected readonly canRequestTransferApproval = this.auth.canInitiateMakerChecker;

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
  protected readonly registering = signal(false);

  protected readonly wizardSteps: WizardStep[] = [
    { labelKey: 'enforcement.wizard.step1' },
    { labelKey: 'enforcement.wizard.step2' },
    { labelKey: 'enforcement.wizard.step3' },
  ];

  protected readonly form = this.fb.nonNullable.group({
    details: this.fb.nonNullable.group({
      customer: this.fb.nonNullable.control<string | null>(null, [Validators.required]),
      bailiff: this.fb.nonNullable.control('', [Validators.required]),
      caseNumber: this.fb.nonNullable.control('', [Validators.required, Validators.minLength(5)]),
    }),
    amounts: this.fb.nonNullable.group(
      {
        kind: this.fb.nonNullable.control<EnforcementKind>('NON_ALIMONY', [Validators.required]),
        blockedAmount: this.fb.nonNullable.control(0, [Validators.required, Validators.min(0)]),
        protectedAmount: this.fb.nonNullable.control(DEFAULT_PROTECTED_AMOUNT, [
          Validators.required,
          Validators.min(0),
        ]),
      },
      { validators: protectedAmountWithinLimitValidator },
    ),
  });

  protected readonly kindOptions: { label: string; value: EnforcementKind }[] = [
    { label: 'enforcement.kind.nonAlimony', value: 'NON_ALIMONY' },
    { label: 'enforcement.kind.alimony', value: 'ALIMONY' },
  ];

  private readonly formTick = toSignal(this.form.valueChanges, { initialValue: null });

  /** Limit kwoty wolnej dla aktualnie wybranego rodzaju zajęcia — do ostrzeżenia w kreatorze. */
  protected readonly selectedKindLimit = computed(() => {
    this.formTick();
    const kind = this.form.controls.amounts.controls.kind.value;
    return PROTECTED_AMOUNT_LIMITS[kind];
  });

  protected get detailsGroup() {
    return this.form.controls.details;
  }

  protected get amountsGroup() {
    return this.form.controls.amounts;
  }

  openWizard(): void {
    this.resetForm();
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
    this.formTick();
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
    this.resetForm();
  }

  async submitWizard(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { details, amounts } = this.form.getRawValue();
    this.registering.set(true);
    try {
      await this.store.register({
        bailiff: details.bailiff,
        caseNumber: details.caseNumber,
        customer: details.customer!,
        kind: amounts.kind,
        blockedAmount: amounts.blockedAmount,
        protectedAmount: amounts.protectedAmount,
      });
      this.notify.success('toast.saved');
      this.wizardVisible.set(false);
      this.wizardStep.set(0);
      this.resetForm();
    } catch (error) {
      // 400 = protectedAmount exceeds the selected kind's limit (backend-enforced, see
      // EnforcementActionsService.validateKindAndProtectedAmount()) — distinct toast from the
      // generic failure so the user knows to lower the amount or change the kind.
      if (error instanceof HttpErrorResponse && error.status === 400) {
        this.notify.error('enforcement.error.protectedAmountExceedsLimit');
      } else {
        this.notify.error('enforcement.error.generic');
      }
    } finally {
      this.registering.set(false);
    }
  }

  // --- F6: wysłanie do zatwierdzenia (maker-checker) ---

  async requestTransferApproval(item: EnforcementCase): Promise<void> {
    if (item.status !== 'registered') {
      this.notify.warn('toast.actionBlocked');
      return;
    }
    try {
      await this.store.requestTransferApproval(item.id);
      this.notify.success('toast.saved');
    } catch (error) {
      this.notify.error(this.resolveErrorKey(error));
    }
  }

  private resolveErrorKey(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 403) {
      return 'enforcement.error.forbidden';
    }
    return 'enforcement.error.generic';
  }

  // --- F6: wykonanie transferu (po zatwierdzeniu) ---

  protected readonly transferDialogOpen = signal(false);
  protected readonly transferCase = signal<EnforcementCase | null>(null);
  protected readonly transferring = signal(false);

  protected readonly transferForm = this.fb.nonNullable.group({
    transferredAmount: this.fb.nonNullable.control<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
    ]),
  });

  private readonly transferFormTick = toSignal(this.transferForm.valueChanges, {
    initialValue: null,
  });

  protected readonly transferFormValid = computed(() => {
    this.transferFormTick();
    return this.transferForm.valid;
  });

  openTransferDialog(item: EnforcementCase): void {
    if (item.status !== 'active') {
      this.notify.warn('toast.actionBlocked');
      return;
    }
    this.transferCase.set(item);
    this.transferForm.reset();
    this.transferDialogOpen.set(true);
  }

  closeTransferDialog(): void {
    this.transferDialogOpen.set(false);
    this.transferCase.set(null);
    this.transferForm.reset();
  }

  async submitTransfer(): Promise<void> {
    const item = this.transferCase();
    if (!item || this.transferForm.invalid) {
      return;
    }
    const { transferredAmount } = this.transferForm.getRawValue();
    this.transferring.set(true);
    try {
      await this.store.transfer(item.id, transferredAmount!);
      this.notify.success('toast.saved');
      this.closeTransferDialog();
    } catch {
      // Most likely 409 (approval not APPROVED yet / no gateway linkage) — see
      // EnforcementActionsService.transfer() preconditions.
      this.notify.error('enforcement.error.transferRejected');
    } finally {
      this.transferring.set(false);
    }
  }

  private resetForm(): void {
    this.form.reset({
      details: { customer: null, bailiff: '', caseNumber: '' },
      amounts: { kind: 'NON_ALIMONY', blockedAmount: 0, protectedAmount: DEFAULT_PROTECTED_AMOUNT },
    });
  }

  // --- Ślad audytowy ("Historia") ---

  protected readonly historyDialogOpen = signal(false);

  openHistoryDialog(item: EnforcementCase): void {
    this.historyDialogOpen.set(true);
    void this.historyStore.load('ENFORCEMENT_CASE', item.id);
  }

  closeHistoryDialog(): void {
    this.historyDialogOpen.set(false);
    this.historyStore.clear();
  }
}
