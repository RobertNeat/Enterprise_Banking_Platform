import { Component, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { NotificationService } from '../../core/services/notification.service';
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { NEUTRAL_TAG_SEVERITY } from '../../shared/utils/status-severity';
import { CARD_AUTHORIZATIONS, PAYMENT_CARDS } from './cards.data';
import { CARD_STATUS_SEVERITY, PaymentCard } from './cards.model';

@Component({
  selector: 'app-cards-page',
  standalone: true,
  imports: [DecimalPipe, ButtonModule, TableModule, TranslatePipe, StatusTagComponent],
  templateUrl: './cards.page.html',
})
export class CardsPage {
  private readonly notify = inject(NotificationService);

  protected readonly cards = PAYMENT_CARDS;
  protected readonly authorizations = CARD_AUTHORIZATIONS;
  protected readonly statusSeverity = CARD_STATUS_SEVERITY;
  protected readonly neutralSeverity = NEUTRAL_TAG_SEVERITY;

  toggleBlock(card: PaymentCard): void {
    if (card.status === 'expired' || card.status === 'pending') {
      this.notify.warn('toast.actionBlocked');
      return;
    }
    card.status = card.status === 'active' ? 'blocked' : 'active';
    this.notify.success('toast.saved');
  }

  fileChargeback(card: PaymentCard): void {
    this.notify.info('cards.chargeback');
  }
}
