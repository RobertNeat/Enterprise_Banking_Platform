import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import {
  EnforcementCase,
  RegisterEnforcementRequest,
  RequestTransferApprovalResponse,
} from './enforcement.model';

/**
 * Zasilane z banking_backend (GET /enforcement). F6 (R4) — register/requestTransferApproval/
 * transfer wołają teraz banking_backend (maker-checker przez governance_mock, transfer środków
 * przez external_institutions_gateway_mock), zamiast tylko lokalnie mutować signal.
 */
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

  /** F6 — registers the case via external_institutions_gateway_mock; registered -> stored with real gateway linkage. */
  async register(request: RegisterEnforcementRequest): Promise<EnforcementCase> {
    const created = await firstValueFrom(
      this.http.post<EnforcementCase>(`${this.apiBaseUrl.bankingBackend}/enforcement`, request),
    );
    this.items.update((list) => [created, ...list]);
    return created;
  }

  /** F6 — opens a governance_mock maker-checker approval for the transfer; registered -> active. */
  async requestTransferApproval(id: string): Promise<RequestTransferApprovalResponse> {
    const response = await firstValueFrom(
      this.http.post<RequestTransferApprovalResponse>(
        `${this.apiBaseUrl.bankingBackend}/enforcement/${id}/request-transfer-approval`,
        {},
      ),
    );
    this.replace(response.enforcementCase);
    return response;
  }

  /** F6 — transfers blocked funds; requires the linked approval to be APPROVED (backend-enforced). */
  async transfer(id: string, transferredAmount: number): Promise<EnforcementCase> {
    const updated = await firstValueFrom(
      this.http.post<EnforcementCase>(
        `${this.apiBaseUrl.bankingBackend}/enforcement/${id}/transfer`,
        {
          transferredAmount,
        },
      ),
    );
    this.replace(updated);
    return updated;
  }

  private replace(enforcementCase: EnforcementCase): void {
    this.items.update((list) =>
      list.map((item) => (item.id === enforcementCase.id ? enforcementCase : item)),
    );
  }
}
