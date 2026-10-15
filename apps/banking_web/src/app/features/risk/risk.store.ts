import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { CollateralEntry } from './risk.model';

/**
 * Zasilane z banking_backend (GET /risk/collateral). F3 — reclassify() przelicza IFRS 9
 * stage/provision przez credit_risk_mock (POST /risk/collateral/{id}/reclassify), daysPastDue
 * jest wpisywane przez operatora (patrz ReclassifyCollateralRequest w banking_backend).
 */
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

  async reclassify(id: string, daysPastDue: number): Promise<CollateralEntry> {
    const updated = await firstValueFrom(
      this.http.post<CollateralEntry>(
        `${this.apiBaseUrl.bankingBackend}/risk/collateral/${id}/reclassify`,
        { daysPastDue },
      ),
    );
    this.items.update((list) => list.map((item) => (item.id === id ? updated : item)));
    return updated;
  }
}
