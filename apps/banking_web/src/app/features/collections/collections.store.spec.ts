import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { COLLECTIONS_CASES } from './collections.data';
import { CollectionsStore } from './collections.store';

describe('CollectionsStore', () => {
  let store: CollectionsStore;
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
    store = TestBed.inject(CollectionsStore);
  });

  afterEach(() => httpMock.verify());

  it('fetches /collections from banking_backend and populates the signal', () => {
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/collections`);
    expect(req.request.method).toBe('GET');
    req.flush(COLLECTIONS_CASES);

    expect(store.cases()).toEqual(COLLECTIONS_CASES);
  });

  it('remind() posts to /collections/{id}/remind and updates the signal', async () => {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/collections`).flush(COLLECTIONS_CASES);
    const item = COLLECTIONS_CASES[4]; // DBT-6580, stage 'monitoring'

    const promise = store.remind(item.id);
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/collections/${item.id}/remind`);
    expect(req.request.method).toBe('POST');
    req.flush(item);
    await promise;

    expect(store.cases().find((c) => c.id === item.id)).toEqual(item);
  });

  it('proposeSettlement() posts to /collections/{id}/propose-settlement and updates the signal', async () => {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/collections`).flush(COLLECTIONS_CASES);
    const item = COLLECTIONS_CASES[4];
    const updated = { ...item, stage: 'settlement' as const };

    const promise = store.proposeSettlement(item.id);
    const req = httpMock.expectOne(
      `${apiBaseUrl.bankingBackend}/collections/${item.id}/propose-settlement`,
    );
    expect(req.request.method).toBe('POST');
    req.flush(updated);
    await promise;

    expect(store.cases().find((c) => c.id === item.id)?.stage).toBe('settlement');
  });

  it('transferExternal() posts to /collections/{id}/transfer-external and updates the signal', async () => {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/collections`).flush(COLLECTIONS_CASES);
    const item = COLLECTIONS_CASES[5]; // DBT-6572, stage 'restructuring'
    const updated = { ...item, stage: 'external' as const };

    const promise = store.transferExternal(item.id);
    const req = httpMock.expectOne(
      `${apiBaseUrl.bankingBackend}/collections/${item.id}/transfer-external`,
    );
    expect(req.request.method).toBe('POST');
    req.flush(updated);
    await promise;

    expect(store.cases().find((c) => c.id === item.id)?.stage).toBe('external');
  });

  it('findActions() gets /collections/{id}/actions', async () => {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/collections`).flush(COLLECTIONS_CASES);
    const item = COLLECTIONS_CASES[0]; // DBT-6601
    const actions = [
      {
        actionType: 'REMINDER_SENT',
        performedAt: '2026-07-15T10:00:00Z',
        performedBy: 'PRC-30678',
        governanceAuditEventId: null,
      },
    ];

    const promise = store.findActions(item.id);
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/collections/${item.id}/actions`);
    expect(req.request.method).toBe('GET');
    req.flush(actions);

    expect(await promise).toEqual(actions);
  });
});
