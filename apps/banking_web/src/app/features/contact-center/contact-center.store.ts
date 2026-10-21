import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import {
  ContactHistoryEntry,
  CustomerOption,
  CustomerProfile,
  ServiceDisposition,
} from './contact-center.model';

/** Zasilane z banking_backend (GET /contact-center/*, GET /customers). Czat (ChatMessage) jest wyłącznie frontendowy. */
@Injectable({ providedIn: 'root' })
export class ContactCenterStore {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(ApiBaseUrlService);

  private readonly customersSignal = signal<CustomerOption[]>([]);
  private readonly selectedCustomerIdSignal = signal<string | null>(null);
  private readonly dispositionsSignal = signal<ServiceDisposition[]>([]);
  private readonly historySignal = signal<ContactHistoryEntry[]>([]);
  private readonly profileSignal = signal<CustomerProfile | null>(null);

  readonly customers = this.customersSignal.asReadonly();
  readonly selectedCustomerId = this.selectedCustomerIdSignal.asReadonly();
  readonly dispositions = this.dispositionsSignal.asReadonly();
  readonly history = this.historySignal.asReadonly();
  readonly profile = this.profileSignal.asReadonly();

  constructor() {
    this.http
      .get<CustomerOption[]>(`${this.apiBaseUrl.bankingBackend}/customers`)
      .subscribe((customers) => {
        this.customersSignal.set(customers);
        if (!this.selectedCustomerIdSignal() && customers.length > 0) {
          this.selectCustomer(customers[0].id);
        }
      });
  }

  /** KKT — wybór klienta zasila widok 360, dyspozycje i historię parametrem customerId. */
  selectCustomer(customerId: string): void {
    this.selectedCustomerIdSignal.set(customerId);

    this.http
      .get<ServiceDisposition[]>(`${this.apiBaseUrl.bankingBackend}/contact-center/dispositions`, {
        params: { customerId },
      })
      .subscribe((dispositions) => {
        if (this.selectedCustomerIdSignal() === customerId) {
          this.dispositionsSignal.set(dispositions);
        }
      });
    this.http
      .get<ContactHistoryEntry[]>(`${this.apiBaseUrl.bankingBackend}/contact-center/history`, {
        params: { customerId },
      })
      .subscribe((history) => {
        if (this.selectedCustomerIdSignal() === customerId) {
          this.historySignal.set(history);
        }
      });
    this.http
      .get<CustomerProfile>(
        `${this.apiBaseUrl.bankingBackend}/contact-center/profile/${customerId}`,
      )
      .subscribe((profile) => {
        if (this.selectedCustomerIdSignal() === customerId) {
          this.profileSignal.set(profile);
        }
      });
  }

  /** KKT/F8 — powiązanie dyspozycji ze sprawą. */
  async linkCase(dispositionId: string, caseId: string): Promise<ServiceDisposition> {
    const updated = await firstValueFrom(
      this.http.put<ServiceDisposition>(
        `${this.apiBaseUrl.bankingBackend}/contact-center/dispositions/${dispositionId}/case`,
        { caseId },
      ),
    );
    this.dispositionsSignal.update((list) =>
      list.map((d) => (d.id === dispositionId ? updated : d)),
    );
    return updated;
  }
}
