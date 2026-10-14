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
});
