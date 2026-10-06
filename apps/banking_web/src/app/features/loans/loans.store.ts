import { Injectable, signal } from '@angular/core';
import { LOAN_APPLICATIONS } from './loans.data';
import { LoanApplication } from './loans.model';

/**
 * Współdzielone źródło danych wniosków kredytowych — używane przez
 * loans.page.ts (lista) i loan-detail.page.ts (szczegóły), żeby nowo
 * utworzony wniosek był widoczny w obu miejscach bez przeładowania strony.
 */
@Injectable({ providedIn: 'root' })
export class LoansStore {
  private readonly items = signal<LoanApplication[]>(LOAN_APPLICATIONS);

  readonly applications = this.items.asReadonly();

  add(loan: LoanApplication): void {
    this.items.update((list) => [loan, ...list]);
  }

  findById(id: string): LoanApplication | undefined {
    return this.items().find((loan) => loan.id === id);
  }
}
