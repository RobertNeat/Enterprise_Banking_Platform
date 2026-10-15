import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { COLLATERAL_ENTRIES } from './risk.data';
import { RiskStore } from './risk.store';

describe('RiskStore', () => {
  let store: RiskStore;
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
    store = TestBed.inject(RiskStore);
  });

  afterEach(() => httpMock.verify());

  it('fetches /risk/collateral from banking_backend and populates the signal', () => {
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/risk/collateral`);
    expect(req.request.method).toBe('GET');
    req.flush(COLLATERAL_ENTRIES);

    expect(store.entries()).toEqual(COLLATERAL_ENTRIES);
  });

  it('reclassify() posts to /risk/collateral/{id}/reclassify and updates the signal', async () => {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/risk/collateral`).flush(COLLATERAL_ENTRIES);
    const entry = COLLATERAL_ENTRIES[0];
    const updated = { ...entry, stage: 'stage2' as const, provision: 31000 };

    const promise = store.reclassify(entry.id, 45);
    const req = httpMock.expectOne(
      `${apiBaseUrl.bankingBackend}/risk/collateral/${entry.id}/reclassify`,
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ daysPastDue: 45 });
    req.flush(updated);
    await promise;

    expect(store.entries().find((e) => e.id === entry.id)).toEqual(updated);
  });
});
