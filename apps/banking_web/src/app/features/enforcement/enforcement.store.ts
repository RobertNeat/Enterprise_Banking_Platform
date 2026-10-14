import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { EnforcementCase } from './enforcement.model';

/** Zasilane z banking_backend (GET /enforcement). Nowe sprawy (wizard) żyją tylko w pamięci. */
@Injectable({ providedIn: 'root' })
export class EnforcementStore {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(ApiBaseUrlService);

  private readonly items = signal<EnforcementCase[]>([]);

  readonly cases = this.items.asReadonly();

  constructor() {
    this.http
      .get<EnforcementCase[]>(`${this.apiBaseUrl.bankingBackend}/enforcement`)
      .subscribe((cases) => this.items.set(cases));
  }

  add(enforcementCase: EnforcementCase): void {
    this.items.update((list) => [enforcementCase, ...list]);
  }
}
