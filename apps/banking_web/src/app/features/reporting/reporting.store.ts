import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { RegulatoryReport } from './reporting.model';

/** Zasilane z banking_backend (GET /reporting/reports). */
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
}
