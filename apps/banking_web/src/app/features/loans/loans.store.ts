import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { LoanApplication } from './loans.model';

/**
 * Współdzielone źródło danych wniosków kredytowych — używane przez
 * loans.page.ts (lista) i loan-detail.page.ts (szczegóły), żeby nowo
 * utworzony wniosek był widoczny w obu miejscach bez przeładowania strony.
 * Zasilane z banking_backend (GET /loans); lokalnie utworzone wnioski (wizard)
 * żyją tylko w pamięci przeglądarki — backend nie wystawia dotąd POST.
 */
@Injectable({ providedIn: 'root' })
export class LoansStore {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(ApiBaseUrlService);

  private readonly items = signal<LoanApplication[]>([]);

  readonly applications = this.items.asReadonly();

  constructor() {
    this.http
      .get<LoanApplication[]>(`${this.apiBaseUrl.bankingBackend}/loans`)
      .subscribe((loans) => this.items.set(loans));
  }

  add(loan: LoanApplication): void {
    this.items.update((list) => [loan, ...list]);
  }

  findById(id: string): LoanApplication | undefined {
    return this.items().find((loan) => loan.id === id);
  }
}
