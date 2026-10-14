import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { REGULATORY_REPORTS } from './reporting.data';
import { ReportingStore } from './reporting.store';

describe('ReportingStore', () => {
  let store: ReportingStore;
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
    store = TestBed.inject(ReportingStore);
  });

  afterEach(() => httpMock.verify());

  it('fetches /reporting/reports from banking_backend and populates the signal', () => {
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/reporting/reports`);
    expect(req.request.method).toBe('GET');
    req.flush(REGULATORY_REPORTS);

    expect(store.reports()).toEqual(REGULATORY_REPORTS);
  });
});
