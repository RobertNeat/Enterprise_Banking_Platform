import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { CollateralEntry } from './risk.model';

/** Zasilane z banking_backend (GET /risk/collateral). */
@Injectable({ providedIn: 'root' })
export class RiskStore {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(ApiBaseUrlService);

  private readonly items = signal<CollateralEntry[]>([]);

  readonly entries = this.items.asReadonly();

  constructor() {
    this.http
      .get<CollateralEntry[]>(`${this.apiBaseUrl.bankingBackend}/risk/collateral`)
      .subscribe((entries) => this.items.set(entries));
  }
}
