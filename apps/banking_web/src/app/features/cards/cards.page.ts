import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TextareaModule } from 'primeng/textarea';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { TranslationService } from '../../core/services/translation.service';
import { NotificationService } from '../../core/services/notification.service';
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { AuditHistoryDialogComponent } from '../../shared/audit-history/audit-history-dialog.component';
import { AuditHistoryStore } from '../../shared/audit-history/audit-history.store';
import { NEUTRAL_TAG_SEVERITY } from '../../shared/utils/status-severity';
import { MoneyPipe } from '../../shared/utils/money.pipe';
import { CardsStore } from './cards.store';
import {
  CARD_STATUS_SEVERITY,
  CHARGEBACK_REASON_CODES,
  CHARGEBACK_STATUS_SEVERITY,
  ChargebackReasonCode,
  PaymentCard,
} from './cards.model';

@Component({
  selector: 'app-cards-page',
  standalone: true,
  imports: [
    MoneyPipe,
    ButtonModule,
    DialogModule,
    SelectModule,
    TableModule,
    TextareaModule,
    ReactiveFormsModule,
    TranslatePipe,
    StatusTagComponent,
    AuditHistoryDialogComponent,
  ],
  templateUrl: './cards.page.html',
})
export class CardsPage {
  private readonly notify = inject(NotificationService);
  private readonly fb = inject(FormBuilder);
  private readonly store = inject(CardsStore);
  protected readonly i18n = inject(TranslationService);
  protected readonly historyStore = inject(AuditHistoryStore);

  protected readonly cards = this.store.cards;
  protected readonly authorizations = this.store.authorizations;
  protected readonly chargebacks = this.store.chargebacks;
  protected readonly statusSeverity = CARD_STATUS_SEVERITY;
  protected readonly chargebackStatusSeverity = CHARGEBACK_STATUS_SEVERITY;
  protected readonly neutralSeverity = NEUTRAL_TAG_SEVERITY;

  protected readonly reasonCodeOptions = computed(() =>
    CHARGEBACK_REASON_CODES.map((code) => ({
      label: this.i18n.translate('cards.chargebackReasonCode.' + code),
      value: code,
    })),
  );

  async toggleBlock(card: PaymentCard): Promise<void> {
    if (card.status === 'expired' || card.status === 'pending') {
      this.notify.warn('toast.actionBlocked');
      return;
    }
    try {
      if (card.status === 'active') {
        await this.store.blockCard(card.id);
      } else {
        await this.store.unblockCard(card.id);
      }
      this.notify.success('toast.saved');
    } catch {
      this.notify.error('cards.error.generic');
    }
  }

  // --- Dialog: zgłoszenie chargebacku ---

  protected readonly chargebackDialogOpen = signal(false);
  protected readonly chargebackCard = signal<PaymentCard | null>(null);
  protected readonly submitting = signal(false);

  protected readonly chargebackForm = this.fb.nonNullable.group({
    authorizationId: this.fb.nonNullable.control<string | null>(null, Validators.required),
    reasonCode: this.fb.nonNullable.control<ChargebackReasonCode | null>(null, Validators.required),
    description: this.fb.nonNullable.control('', Validators.required),
  });

  private readonly chargebackFormTick = toSignal(this.chargebackForm.valueChanges, {
    initialValue: null,
  });

  protected readonly chargebackFormValid = computed(() => {
    this.chargebackFormTick();
    return this.chargebackForm.valid;
  });

  protected readonly authorizationOptionsForCard = computed(() => {
    const card = this.chargebackCard();
    if (!card) {
      return [];
    }
    return this.authorizations()
      .filter((auth) => auth.cardId === card.id)
      .map((auth) => ({
        label: `${auth.merchant} — ${auth.amount} PLN (${auth.date})`,
        value: auth.id,
      }));
  });

  fileChargeback(card: PaymentCard): void {
    this.chargebackCard.set(card);
    this.chargebackForm.reset();
    this.chargebackDialogOpen.set(true);
  }

  closeChargebackDialog(): void {
    this.chargebackDialogOpen.set(false);
    this.chargebackCard.set(null);
    this.chargebackForm.reset();
  }

  async submitChargeback(): Promise<void> {
    const card = this.chargebackCard();
    if (!card || this.chargebackForm.invalid) {
      return;
    }
    const value = this.chargebackForm.getRawValue();
    this.submitting.set(true);
    try {
      await this.store.fileChargeback(card.id, {
        authorizationId: value.authorizationId!,
        reasonCode: value.reasonCode!,
        description: value.description,
      });
      this.notify.success('toast.saved');
      this.closeChargebackDialog();
    } catch {
      this.notify.error('cards.error.generic');
    } finally {
      this.submitting.set(false);
    }
  }

  chargebacksForAuthorization(authorizationId: string) {
    return this.chargebacks().filter((cb) => cb.cardAuthorizationId === authorizationId);
  }

  // --- Ślad audytowy ("Historia") ---

  protected readonly historyDialogOpen = signal(false);

  openHistoryDialog(card: PaymentCard): void {
    this.historyDialogOpen.set(true);
    void this.historyStore.load('PAYMENT_CARD', card.id);
  }

  closeHistoryDialog(): void {
    this.historyDialogOpen.set(false);
    this.historyStore.clear();
  }
}
