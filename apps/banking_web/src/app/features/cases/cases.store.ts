import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { CustomerCase } from './cases.model';

/** Zasilane z banking_backend (GET /cases). Mutacje (escalate/add) żyją tylko w pamięci. */
@Injectable({ providedIn: 'root' })
export class CasesStore {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(ApiBaseUrlService);

  private readonly items = signal<CustomerCase[]>([]);

  readonly cases = this.items.asReadonly();

  constructor() {
    this.http
      .get<CustomerCase[]>(`${this.apiBaseUrl.bankingBackend}/cases`)
      .subscribe((cases) => this.items.set(cases));
  }

  add(customerCase: CustomerCase): void {
    this.items.update((list) => [customerCase, ...list]);
  }

  update(id: string, updater: (item: CustomerCase) => CustomerCase): void {
    this.items.update((list) => list.map((c) => (c.id === id ? updater(c) : c)));
  }
}
