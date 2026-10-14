import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { CollectionsCase } from './collections.model';

/** Zasilane z banking_backend (GET /collections). */
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

  update(id: string, updater: (item: CollectionsCase) => CollectionsCase): void {
    this.items.update((list) => list.map((c) => (c.id === id ? updater(c) : c)));
  }
}
