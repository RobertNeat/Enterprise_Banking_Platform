import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { Chargeback, CardAuthorization, FileChargebackRequest, PaymentCard } from './cards.model';

/**
 * Zasilane z banking_backend (GET /cards, GET /cards/authorizations, GET /cards/chargebacks).
 * Mutacje (block/unblock/chargeback filing/advancement) F4 przechodzą przez card_processor_mock
 * (proxy w banking_backend) — zobacz docs/services/banking_backend.md.
 */
@Injectable({ providedIn: 'root' })
export class CardsStore {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(ApiBaseUrlService);

  private readonly cardsSignal = signal<PaymentCard[]>([]);
  private readonly authorizationsSignal = signal<CardAuthorization[]>([]);
  private readonly chargebacksSignal = signal<Chargeback[]>([]);

  readonly cards = this.cardsSignal.asReadonly();
  readonly authorizations = this.authorizationsSignal.asReadonly();
  readonly chargebacks = this.chargebacksSignal.asReadonly();

  constructor() {
    this.http
      .get<PaymentCard[]>(`${this.apiBaseUrl.bankingBackend}/cards`)
      .subscribe((cards) => this.cardsSignal.set(cards));
    this.http
      .get<CardAuthorization[]>(`${this.apiBaseUrl.bankingBackend}/cards/authorizations`)
      .subscribe((authorizations) => this.authorizationsSignal.set(authorizations));
    this.http
      .get<Chargeback[]>(`${this.apiBaseUrl.bankingBackend}/cards/chargebacks`)
      .subscribe((chargebacks) => this.chargebacksSignal.set(chargebacks));
  }

  /** F4 — blocks the card via card_processor_mock; backend rejects (409) if not currently 'active'. */
  async blockCard(cardId: string): Promise<PaymentCard> {
    const updated = await firstValueFrom(
      this.http.post<PaymentCard>(`${this.apiBaseUrl.bankingBackend}/cards/${cardId}/block`, {}),
    );
    this.replaceCard(updated);
    return updated;
  }

  /** F4 — unblocks the card via card_processor_mock; backend rejects (409) if not currently 'blocked'. */
  async unblockCard(cardId: string): Promise<PaymentCard> {
    const updated = await firstValueFrom(
      this.http.post<PaymentCard>(`${this.apiBaseUrl.bankingBackend}/cards/${cardId}/unblock`, {}),
    );
    this.replaceCard(updated);
    return updated;
  }

  /** F4 — files a chargeback against one of this card's authorizations via card_processor_mock. */
  async fileChargeback(cardId: string, request: FileChargebackRequest): Promise<Chargeback> {
    const created = await firstValueFrom(
      this.http.post<Chargeback>(
        `${this.apiBaseUrl.bankingBackend}/cards/${cardId}/chargebacks`,
        request,
      ),
    );
    this.chargebacksSignal.update((list) => [created, ...list]);
    return created;
  }

  private replaceCard(card: PaymentCard): void {
    this.cardsSignal.update((list) => list.map((item) => (item.id === card.id ? card : item)));
  }
}
