import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { EbankingUser } from './ebanking.model';

/** Zasilane z banking_backend (GET /ebanking/users). */
@Injectable({ providedIn: 'root' })
export class EbankingStore {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(ApiBaseUrlService);

  private readonly items = signal<EbankingUser[]>([]);

  readonly users = this.items.asReadonly();

  constructor() {
    this.http
      .get<EbankingUser[]>(`${this.apiBaseUrl.bankingBackend}/ebanking/users`)
      .subscribe((users) => this.items.set(users));
  }
}
