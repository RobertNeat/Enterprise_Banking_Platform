import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { CollectionsCase } from './collections.model';

/**
 * Zasilane z banking_backend (GET /collections). F3 (R4) — remind/proposeSettlement/
 * transferExternal wołają teraz banking_backend (stage preconditions przeniesione na backend,
 * patrz CollectionsActionsService), zamiast tylko lokalnie mutować signal.
 */
@Injectable({ providedIn: 'root' })
export class CollectionsStore {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(ApiBaseUrlService);

  private readonly items = signal<CollectionsCase[]>([]);

  readonly cases = this.items.asReadonly();

  constructor() {
    this.http
      .get<CollectionsCase[]>(`${this.apiBaseUrl.bankingBackend}/collections`)
      .subscribe((cases) => this.items.set(cases));
  }

  /** No stage change — backend only audits; 409 if the case is already in stage 'legal'. */
  async remind(id: string): Promise<CollectionsCase> {
    return this.postAction(id, 'remind');
  }

  async proposeSettlement(id: string): Promise<CollectionsCase> {
    return this.postAction(id, 'propose-settlement');
  }

  /** 409 if the case is still 'monitoring'/'reminder'. */
  async transferExternal(id: string): Promise<CollectionsCase> {
    return this.postAction(id, 'transfer-external');
  }

  private async postAction(id: string, action: string): Promise<CollectionsCase> {
    const updated = await firstValueFrom(
      this.http.post<CollectionsCase>(
        `${this.apiBaseUrl.bankingBackend}/collections/${id}/${action}`,
        {},
      ),
    );
    this.items.update((list) => list.map((c) => (c.id === id ? updated : c)));
    return updated;
  }
}
