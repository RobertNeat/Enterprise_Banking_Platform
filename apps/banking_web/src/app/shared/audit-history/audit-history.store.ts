import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { NotificationService } from '../../core/services/notification.service';
import { AuditEvent } from './audit-event.model';

/**
 * Zasilane z banking_backend (proxy do governance_mock GET /audit-events). Jeden store
 * współdzielony przez wszystkie dialogi "Historia" (loan/case/card/enforcement) — zdarzenia są
 * zawsze scope'owane do jednej encji (entityType+entityId), więc trzymanie listy per-store nie
 * wymaga osobnej instancji na widok.
 *
 * load() łapie błędy sam (zamiast propagować do wołających stron) — każda strona woła go jako
 * fire-and-forget (`void this.historyStore.load(...)`) od razu po otwarciu dialogu, więc bez tego
 * odrzucenie promise'a (np. 503 z governance_mock) byłoby nieobsłużone.
 */
@Injectable({ providedIn: 'root' })
export class AuditHistoryStore {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(ApiBaseUrlService);
  private readonly notify = inject(NotificationService);

  private readonly items = signal<AuditEvent[]>([]);
  private readonly loadingState = signal(false);

  readonly events = this.items.asReadonly();
  readonly loading = this.loadingState.asReadonly();

  async load(entityType: string, entityId: string): Promise<void> {
    this.loadingState.set(true);
    try {
      const params = new HttpParams().set('entityType', entityType).set('entityId', entityId);
      const result = await firstValueFrom(
        this.http.get<AuditEvent[]>(`${this.apiBaseUrl.bankingBackend}/audit-events`, { params }),
      );
      this.items.set(result);
    } catch {
      // Most likely a 503 (governance_mock unavailable) — see GovernanceUnavailableException.
      this.items.set([]);
      this.notify.error('history.error.unavailable');
    } finally {
      this.loadingState.set(false);
    }
  }

  clear(): void {
    this.items.set([]);
  }
}
