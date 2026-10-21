import { Component, inject, input, output, signal } from '@angular/core';
import { DialogModule } from 'primeng/dialog';
import { StepperModule } from 'primeng/stepper';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { TranslationService } from '../../core/services/translation.service';

export interface WizardStep {
  /** Krótka etykieta kroku (klucz i18n) widoczna w p-step-list. */
  labelKey: string;
}

/**
 * Współdzielony wizard formularzowy: p-dialog + p-stepper z nawigowalnymi
 * krokami (w przeciwieństwie do read-only steppera lifecycle, patrz
 * docs/banking_web_ui_conventions.md). Treść każdego kroku jest
 * projektowana przez wywołującego (content projection, patrz `@switch` w
 * stronach korzystających z komponentu); komponent odpowiada tylko za
 * nawigację, walidację per-krok (stepValid()) i layout Next/Back/Submit/
 * Cancel. Każde zamknięcie bez zatwierdzenia (przycisk Cancel, X w
 * headerze, klawisz Escape) przechodzi przez jedno potwierdzenie
 * (ConfirmDialog) — `visible` jest bindowane jednostronnie z sygnału
 * strony, więc dopóki strona nie ustawi go na false, dialog PrimeNG wraca
 * do widocznego stanu w kolejnym cyklu detekcji zmian.
 */
@Component({
  selector: 'app-wizard-dialog',
  standalone: true,
  imports: [DialogModule, StepperModule, ButtonModule, ConfirmDialogModule, TranslatePipe],
  providers: [ConfirmationService],
  template: `
    <p-dialog
      [id]="dialogId()"
      [pt]="{ pcCloseButton: { root: { id: dialogId() + '-close-btn' } } }"
      [visible]="visible()"
      (visibleChange)="onVisibleChange($event)"
      [modal]="true"
      [closable]="true"
      [style]="{ width: '48rem', maxWidth: '95vw' }"
      [header]="titleKey() | translate"
    >
      <p-stepper [id]="dialogId() + '-stepper'" [value]="activeIndex()" [linear]="true">
        <p-step-list [id]="dialogId() + '-step-list'">
          @for (step of steps(); track step.labelKey; let i = $index) {
            <p-step
              [id]="dialogId() + '-step-' + i"
              [pt]="{ header: { id: dialogId() + '-step-' + i + '-header' } }"
              [value]="i"
              [disabled]="true"
            >
              {{ step.labelKey | translate }}
            </p-step>
          }
        </p-step-list>
      </p-stepper>

      <div [id]="dialogId() + '-step-content'" class="wizard-dialog__content">
        <ng-content />
      </div>

      <div [id]="dialogId() + '-actions'" class="wizard-dialog__actions">
        <p-button
          [id]="dialogId() + '-cancel-btn'"
          [pt]="{ root: { id: dialogId() + '-cancel-btn-root' } }"
          [label]="'wizard.cancel' | translate"
          severity="secondary"
          [text]="true"
          (click)="requestCancel()"
        />
        <div class="wizard-dialog__actions-right">
          @if (activeIndex() > 0) {
            <p-button
              [id]="dialogId() + '-back-btn'"
              [pt]="{ root: { id: dialogId() + '-back-btn-root' } }"
              [label]="'wizard.back' | translate"
              severity="secondary"
              (click)="goBack()"
            />
          }
          @if (activeIndex() < steps().length - 1) {
            <p-button
              [id]="dialogId() + '-next-btn'"
              [pt]="{ root: { id: dialogId() + '-next-btn-root' } }"
              [label]="'wizard.next' | translate"
              icon="pi pi-arrow-right"
              iconPos="right"
              [disabled]="!stepValid()"
              (click)="goNext()"
            />
          } @else {
            <p-button
              [id]="dialogId() + '-submit-btn'"
              [pt]="{ root: { id: dialogId() + '-submit-btn-root' } }"
              [label]="'wizard.submit' | translate"
              icon="pi pi-check"
              [disabled]="!stepValid()"
              (click)="wizardSubmit.emit()"
            />
          }
        </div>
      </div>
    </p-dialog>

    <p-confirmdialog
      [id]="dialogId() + '-cancel-confirm'"
      [key]="dialogId() + '-cancel-confirm'"
      [pt]="{
        pcAcceptButton: { root: { id: dialogId() + '-cancel-confirm-accept-btn' } },
        pcRejectButton: { root: { id: dialogId() + '-cancel-confirm-reject-btn' } },
      }"
    />
  `,
  styles: `
    .wizard-dialog__content {
      min-height: 14rem;
      padding: 1rem 0;
    }
    .wizard-dialog__actions {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .wizard-dialog__actions-right {
      display: flex;
      gap: 0.5rem;
    }
  `,
})
export class WizardDialogComponent {
  /** Prefiks id dla dialogu i wszystkich elementów wewnętrznych. */
  readonly dialogId = input.required<string>();
  readonly titleKey = input.required<string>();
  readonly steps = input.required<WizardStep[]>();
  readonly visible = input.required<boolean>();
  /** Czy aktualny krok jest poprawny — blokuje Next/Submit, gdy false. */
  readonly stepValid = input.required<boolean>();

  readonly visibleChange = output<boolean>();
  readonly stepChange = output<number>();
  readonly wizardCancel = output<void>();
  readonly wizardSubmit = output<void>();

  private readonly confirmation = inject(ConfirmationService);
  private readonly i18n = inject(TranslationService);

  protected readonly activeIndex = signal(0);

  onVisibleChange(value: boolean): void {
    // p-dialog próbuje się zamknąć samo (X w headerze, Escape) ustawiając
    // swój wewnętrzny [visible] na false i emitując to zdarzenie — ale
    // [visible] na tym komponencie jest bindowane JEDNOSTRONNIE z sygnału
    // strony (visible()), więc jeśli strona nie ustawi go na false, kolejny
    // cykl detekcji zmian przywróci dialog jako widoczny. Dzięki temu każde
    // zamknięcie (X, Escape, przycisk Cancel) może iść przez to samo
    // potwierdzenie, bez realnego zamykania dialogu "pod spodem".
    if (!value) {
      this.requestCancel();
      return;
    }
    this.visibleChange.emit(value);
  }

  requestCancel(): void {
    this.confirmation.confirm({
      key: this.dialogId() + '-cancel-confirm',
      header: this.i18n.translate('wizard.cancelConfirm.header'),
      message: this.i18n.translate('wizard.cancelConfirm.message'),
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: this.i18n.translate('wizard.cancelConfirm.accept'),
      rejectLabel: this.i18n.translate('wizard.cancelConfirm.reject'),
      accept: () => {
        this.activeIndex.set(0);
        this.wizardCancel.emit();
      },
    });
  }

  goNext(): void {
    if (!this.stepValid()) {
      return;
    }
    this.activeIndex.update((i) => Math.min(i + 1, this.steps().length - 1));
    this.stepChange.emit(this.activeIndex());
  }

  goBack(): void {
    this.activeIndex.update((i) => Math.max(i - 1, 0));
    this.stepChange.emit(this.activeIndex());
  }

  /** Resetuje krok do pierwszego — wywoływane przez stronę przy otwarciu wizarda. */
  reset(): void {
    this.activeIndex.set(0);
  }

  currentStep(): number {
    return this.activeIndex();
  }
}
