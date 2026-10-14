import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { CardAuthorization, PaymentCard } from './cards.model';

/** Zasilane z banking_backend (GET /cards, GET /cards/authorizations). */
@Injectable({ providedIn: 'root' })
export class CardsStore {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(ApiBaseUrlService);

  private readonly cardsSignal = signal<PaymentCard[]>([]);
  private readonly authorizationsSignal = signal<CardAuthorization[]>([]);

  readonly cards = this.cardsSignal.asReadonly();
  readonly authorizations = this.authorizationsSignal.asReadonly();

  constructor() {
    this.http
      .get<PaymentCard[]>(`${this.apiBaseUrl.bankingBackend}/cards`)
      .subscribe((cards) => this.cardsSignal.set(cards));
    this.http
      .get<CardAuthorization[]>(`${this.apiBaseUrl.bankingBackend}/cards/authorizations`)
      .subscribe((authorizations) => this.authorizationsSignal.set(authorizations));
  }
}
