import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { ContactHistoryEntry, CustomerProfile, ServiceDisposition } from './contact-center.model';

/** Domyślny klient profilu — dzisiejszy statyczny widok zawsze pokazuje CUST-001. */
const DEFAULT_PROFILE_CUSTOMER_ID = 'CUST-001';

/** Zasilane z banking_backend (GET /contact-center/*). Czat (ChatMessage) jest wyłącznie frontendowy. */
@Injectable({ providedIn: 'root' })
export class ContactCenterStore {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(ApiBaseUrlService);

  private readonly dispositionsSignal = signal<ServiceDisposition[]>([]);
  private readonly historySignal = signal<ContactHistoryEntry[]>([]);
  private readonly profileSignal = signal<CustomerProfile | null>(null);

  readonly dispositions = this.dispositionsSignal.asReadonly();
  readonly history = this.historySignal.asReadonly();
  readonly profile = this.profileSignal.asReadonly();

  constructor() {
    this.http
      .get<ServiceDisposition[]>(`${this.apiBaseUrl.bankingBackend}/contact-center/dispositions`)
      .subscribe((dispositions) => this.dispositionsSignal.set(dispositions));
    this.http
      .get<ContactHistoryEntry[]>(`${this.apiBaseUrl.bankingBackend}/contact-center/history`)
      .subscribe((history) => this.historySignal.set(history));
    this.http
      .get<CustomerProfile>(
        `${this.apiBaseUrl.bankingBackend}/contact-center/profile/${DEFAULT_PROFILE_CUSTOMER_ID}`,
      )
      .subscribe((profile) => this.profileSignal.set(profile));
  }
}
