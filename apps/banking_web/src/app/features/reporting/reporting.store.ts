import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import {
  RefreshSubmissionStatusResponse,
  RegulatoryReport,
  ReportSubmission,
} from './reporting.model';

/**
 * Zasilane z banking_backend (GET /reporting/reports). F7 (R4) — submit()/refreshSubmissionStatus()
 * wołają external_institutions_gateway_mock przez banking_backend (manualne przyciski, nie
 * polling — prościej testować deterministycznie, patrz docs/services/banking_backend.md).
 */
@Injectable({ providedIn: 'root' })
export class ReportingStore {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(ApiBaseUrlService);

  private readonly items = signal<RegulatoryReport[]>([]);

  readonly reports = this.items.asReadonly();

  constructor() {
    this.http
      .get<RegulatoryReport[]>(`${this.apiBaseUrl.bankingBackend}/reporting/reports`)
      .subscribe((reports) => this.items.set(reports));
  }

  /** F7 — submits a draft report via the gateway; 409 unless status 'draft'. */
  async submit(id: string): Promise<RegulatoryReport> {
    const updated = await firstValueFrom(
      this.http.post<RegulatoryReport>(
        `${this.apiBaseUrl.bankingBackend}/reporting/reports/${id}/submit`,
        {},
      ),
    );
    this.replace(updated);
    return updated;
  }

  /** F7 — polls the gateway for the latest accept/reject outcome; 409 unless status 'submitted'. */
  async refreshSubmissionStatus(id: string): Promise<RefreshSubmissionStatusResponse> {
    const response = await firstValueFrom(
      this.http.post<RefreshSubmissionStatusResponse>(
        `${this.apiBaseUrl.bankingBackend}/reporting/reports/${id}/refresh-submission-status`,
        {},
      ),
    );
    this.replace(response.report);
    return response;
  }

  /** GET /reporting/reports/{id}/submissions — historia zgłoszeń do bramy (panel prezentacyjny). */
  async findSubmissions(id: string): Promise<ReportSubmission[]> {
    return firstValueFrom(
      this.http.get<ReportSubmission[]>(
        `${this.apiBaseUrl.bankingBackend}/reporting/reports/${id}/submissions`,
      ),
    );
  }

  private replace(report: RegulatoryReport): void {
    this.items.update((list) => list.map((item) => (item.id === report.id ? report : item)));
  }
}
