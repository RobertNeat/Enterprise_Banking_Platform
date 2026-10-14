import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { CUSTOMER_CASES } from './cases.data';
import { EscalateCaseResponse } from './cases.model';
import { CasesStore } from './cases.store';

describe('CasesStore', () => {
  let store: CasesStore;
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
    store = TestBed.inject(CasesStore);
  });

  afterEach(() => httpMock.verify());

  it('fetches /cases from banking_backend and populates the signal', () => {
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/cases`);
    expect(req.request.method).toBe('GET');
    req.flush(CUSTOMER_CASES);

    expect(store.cases()).toEqual(CUSTOMER_CASES);
  });

  it('update replaces a single case in place', () => {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/cases`).flush(CUSTOMER_CASES);

    store.update(CUSTOMER_CASES[0].id, (c) => ({ ...c, status: 'escalated' }));
    expect(store.cases()[0].status).toBe('escalated');
  });

  it('create() posts to /cases and prepends the backend-computed result', async () => {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/cases`).flush([]);

    const created = { ...CUSTOMER_CASES[0], id: 'CS-90001', status: 'new' as const };
    const promise = store.create({
      subject: 'Reklamacja',
      customerId: 'CUST-001',
      priority: 'high',
      caseType: 'complaint',
    });
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/cases`);
    expect(req.request.method).toBe('POST');
    req.flush(created);
    const result = await promise;

    expect(result).toEqual(created);
    expect(store.cases()[0].id).toBe('CS-90001');
  });

  it('escalate() posts and replaces the case with the updated status', async () => {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/cases`).flush(CUSTOMER_CASES);

    const response: EscalateCaseResponse = {
      customerCase: { ...CUSTOMER_CASES[0], status: 'escalated' },
      escalated: true,
    };
    const promise = store.escalate(CUSTOMER_CASES[0].id);
    const req = httpMock.expectOne(
      `${apiBaseUrl.bankingBackend}/cases/${CUSTOMER_CASES[0].id}/escalate`,
    );
    expect(req.request.method).toBe('POST');
    req.flush(response);
    const result = await promise;

    expect(result.escalated).toBe(true);
    expect(store.cases()[0].status).toBe('escalated');
  });
});
