import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { EmployeeAccountView } from './accounts.model';

/** Zasilane z banking_backend (proxy do identity_access_mock /auth/accounts*, gate: accounts-admin). */
@Injectable({ providedIn: 'root' })
export class AccountsStore {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(ApiBaseUrlService);

  private readonly items = signal<EmployeeAccountView[]>([]);

  readonly accounts = this.items.asReadonly();

  async load(): Promise<void> {
    const result = await firstValueFrom(
      this.http.get<EmployeeAccountView[]>(`${this.apiBaseUrl.bankingBackend}/auth/accounts`),
    );
    this.items.set(result);
  }

  async approve(id: string, role: string): Promise<void> {
    const updated = await firstValueFrom(
      this.http.post<EmployeeAccountView>(
        `${this.apiBaseUrl.bankingBackend}/auth/accounts/${id}/approve`,
        { role },
      ),
    );
    this.items.update((list) => list.map((a) => (a.id === id ? updated : a)));
  }

  async reject(id: string): Promise<void> {
    const updated = await firstValueFrom(
      this.http.post<EmployeeAccountView>(
        `${this.apiBaseUrl.bankingBackend}/auth/accounts/${id}/reject`,
        {},
      ),
    );
    this.items.update((list) => list.map((a) => (a.id === id ? updated : a)));
  }
}
