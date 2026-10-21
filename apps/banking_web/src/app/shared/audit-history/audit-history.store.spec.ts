import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { MessageService } from 'primeng/api';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { NotificationService } from '../../core/services/notification.service';
import { AuditEvent } from './audit-event.model';
import { AuditHistoryStore } from './audit-history.store';

const EVENTS: AuditEvent[] = [
  {
    id: 'evt-1',
    actor: 'PRC-10234',
    action: 'LOAN_STAGE_CHANGED',
    entityType: 'LOAN_APPLICATION',
    entityId: 'LN-80203',
    timestamp: '2026-10-10T08:00:00Z',
    details: 'application -> decision',
  },
];

describe('AuditHistoryStore', () => {
  let store: AuditHistoryStore;
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), MessageService],
    });
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
    store = TestBed.inject(AuditHistoryStore);
  });

  afterEach(() => httpMock.verify());

  it('load() fetches /audit-events with entityType and entityId as query params', async () => {
    const promise = store.load('LOAN_APPLICATION', 'LN-80203');
    const req = httpMock.expectOne(
      (r) =>
        r.url === `${apiBaseUrl.bankingBackend}/audit-events` &&
        r.params.get('entityType') === 'LOAN_APPLICATION' &&
        r.params.get('entityId') === 'LN-80203',
    );
    expect(req.request.method).toBe('GET');
    req.flush(EVENTS);
    await promise;

    expect(store.events()).toEqual(EVENTS);
    expect(store.loading()).toBe(false);
  });

  it('loading() is true while the request is in flight', async () => {
    const promise = store.load('CUSTOMER_CASE', 'CASE-1');
    expect(store.loading()).toBe(true);
    const req = httpMock.expectOne((r) => r.url === `${apiBaseUrl.bankingBackend}/audit-events`);
    req.flush([]);
    await promise;

    expect(store.loading()).toBe(false);
  });

  it('clear() resets the events list', async () => {
    const promise = store.load('PAYMENT_CARD', 'CARD-1');
    httpMock
      .expectOne(
        `${apiBaseUrl.bankingBackend}/audit-events?entityType=PAYMENT_CARD&entityId=CARD-1`,
      )
      .flush(EVENTS);
    await promise;
    expect(store.events().length).toBe(1);

    store.clear();
    expect(store.events()).toEqual([]);
  });

  it('load() on a 503 clears the events and shows an error toast instead of rejecting', async () => {
    const errorSpy = vi.spyOn(TestBed.inject(NotificationService), 'error');

    const promise = store.load('ENFORCEMENT_CASE', 'ENF-1');
    const req = httpMock.expectOne((r) => r.url === `${apiBaseUrl.bankingBackend}/audit-events`);
    req.flush(
      { message: 'governance_mock unavailable' },
      { status: 503, statusText: 'Service Unavailable' },
    );

    // load() must not reject/throw out to the caller — callers do `void this.historyStore.load(...)`.
    await promise;

    expect(store.events()).toEqual([]);
    expect(store.loading()).toBe(false);
    expect(errorSpy).toHaveBeenCalledWith('history.error.unavailable');
  });
});
