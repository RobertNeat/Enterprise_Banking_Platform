import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { Approval, ApprovalStatus } from './approvals.model';

/** Zasilane z banking_backend (proxy do governance_mock /approvals). Decyzje mutuja przez backend. */
@Injectable({ providedIn: 'root' })
export class ApprovalsStore {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(ApiBaseUrlService);

  private readonly items = signal<Approval[]>([]);

  readonly approvals = this.items.asReadonly();

  async load(status?: ApprovalStatus | null): Promise<void> {
    const params = status ? new HttpParams().set('status', status) : undefined;
    const result = await firstValueFrom(
      this.http.get<Approval[]>(`${this.apiBaseUrl.bankingBackend}/approvals`, { params }),
    );
    this.items.set(result);
  }

  async decide(id: string, decision: 'APPROVED' | 'REJECTED'): Promise<void> {
    const updated = await firstValueFrom(
      this.http.post<Approval>(`${this.apiBaseUrl.bankingBackend}/approvals/${id}/decide`, {
        decision,
      }),
    );
    this.items.update((list) => list.map((a) => (a.id === id ? updated : a)));
  }
}
