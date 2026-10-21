import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import {
  CreateLoanApplicationRequest,
  LoanApplication,
  LoanDetail,
  SubmitForApprovalResponse,
} from './loans.model';

/**
 * Shared loan application source — used by loans.page.ts (list) and loan-detail.page.ts
 * (details), so a newly submitted loan is visible in both without a page reload. Feeds from
 * banking_backend (GET /loans); mutations (create/submit-for-approval/advance-stage) go through
 * the backend too (F2 — scoring, maker-checker approval, installment generation), not local-only.
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

  findById(id: string): LoanApplication | undefined {
    return this.items().find((loan) => loan.id === id);
  }

  /** F2 — submits the application; scoreResult/dsti in the response come from credit_risk_mock. */
  async create(request: CreateLoanApplicationRequest): Promise<LoanDetail> {
    const detail = await firstValueFrom(
      this.http.post<LoanDetail>(`${this.apiBaseUrl.bankingBackend}/loans`, request),
    );
    this.items.update((list) => [detail, ...list]);
    return detail;
  }

  /** F2 — application -> decision: opens a governance_mock maker-checker approval. */
  async submitForApproval(id: string): Promise<SubmitForApprovalResponse> {
    const response = await firstValueFrom(
      this.http.post<SubmitForApprovalResponse>(
        `${this.apiBaseUrl.bankingBackend}/loans/${id}/submit-for-approval`,
        {},
      ),
    );
    this.replace(response.loan);
    return response;
  }

  /**
   * F2 — decision -> contract: requires the linked approval to be APPROVED; generates the
   * schedule. A REJECTED approval instead comes back with stage 'application' (see
   * LoanActionsService.advanceStage) so the caller can offer resubmission instead of an error.
   */
  async advanceStage(id: string): Promise<LoanDetail> {
    const detail = await firstValueFrom(
      this.http.post<LoanDetail>(`${this.apiBaseUrl.bankingBackend}/loans/${id}/advance-stage`, {}),
    );
    this.replace(detail);
    return detail;
  }

  /** F2 — contract -> disbursement -> schedule: operational payout step after the contract stage. */
  async disburse(id: string): Promise<LoanDetail> {
    const detail = await firstValueFrom(
      this.http.post<LoanDetail>(`${this.apiBaseUrl.bankingBackend}/loans/${id}/disburse`, {}),
    );
    this.replace(detail);
    return detail;
  }

  private replace(loan: LoanApplication): void {
    this.items.update((list) => {
      const exists = list.some((item) => item.id === loan.id);
      return exists ? list.map((item) => (item.id === loan.id ? loan : item)) : [loan, ...list];
    });
  }
}
