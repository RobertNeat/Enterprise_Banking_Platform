import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { ENFORCEMENT_CASES } from './enforcement.data';
import { EnforcementStore } from './enforcement.store';

describe('EnforcementStore', () => {
  let store: EnforcementStore;
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
    store = TestBed.inject(EnforcementStore);
  });

  afterEach(() => httpMock.verify());

  it('fetches /enforcement from banking_backend and populates the signal', () => {
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/enforcement`);
    expect(req.request.method).toBe('GET');
    req.flush(ENFORCEMENT_CASES);

    expect(store.cases()).toEqual(ENFORCEMENT_CASES);
  });

  it('add prepends a locally created case without another HTTP call', () => {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/enforcement`).flush([]);

    store.add(ENFORCEMENT_CASES[0]);
    expect(store.cases()).toEqual([ENFORCEMENT_CASES[0]]);
  });
});
