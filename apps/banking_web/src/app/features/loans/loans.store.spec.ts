import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { LOAN_APPLICATIONS } from './loans.data';
import { LoansStore } from './loans.store';

describe('LoansStore', () => {
  let store: LoansStore;
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
    store = TestBed.inject(LoansStore);
  });

  afterEach(() => httpMock.verify());

  it('fetches /loans from banking_backend and populates the signal', () => {
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans`);
    expect(req.request.method).toBe('GET');
    req.flush(LOAN_APPLICATIONS);

    expect(store.applications()).toEqual(LOAN_APPLICATIONS);
  });

  it('findById resolves a loan loaded from the backend', () => {
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans`);
    req.flush(LOAN_APPLICATIONS);

    expect(store.findById('LN-80231')?.applicant).toBe('Jan Wiśniewski');
  });

  it('add prepends a locally created loan without another HTTP call', () => {
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans`);
    req.flush([]);

    store.add(LOAN_APPLICATIONS[0]);
    expect(store.applications()).toEqual([LOAN_APPLICATIONS[0]]);
  });
});
