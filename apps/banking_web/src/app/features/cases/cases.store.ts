import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { CreateCustomerCaseRequest, CustomerCase, EscalateCaseResponse } from './cases.model';

/**
 * Zasilane z banking_backend (GET /cases). create()/escalate() mutują przez backend — F8:
 * statutoryDeadline/slaRemainingHours są wyliczane po stronie backendu (POST /cases), eskalacja
 * (POST /cases/{id}/escalate) ocenia SLA przez governance_mock.
 */
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

  update(id: string, updater: (item: CustomerCase) => CustomerCase): void {
    this.items.update((list) => list.map((c) => (c.id === id ? updater(c) : c)));
  }

  /** F8 — statutoryDeadline/slaRemainingHours in the response come from banking_backend's own rule. */
  async create(request: CreateCustomerCaseRequest): Promise<CustomerCase> {
    const created = await firstValueFrom(
      this.http.post<CustomerCase>(`${this.apiBaseUrl.bankingBackend}/cases`, request),
    );
    this.items.update((list) => [created, ...list]);
    return created;
  }

  /** F8 — evaluates SLA via governance_mock; backend decides auto vs. manual escalation audit. */
  async escalate(id: string): Promise<EscalateCaseResponse> {
    const response = await firstValueFrom(
      this.http.post<EscalateCaseResponse>(
        `${this.apiBaseUrl.bankingBackend}/cases/${id}/escalate`,
        {},
      ),
    );
    this.update(id, () => response.customerCase);
    return response;
  }
}
