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

  it('register() posts to /enforcement and prepends the created case', async () => {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/enforcement`).flush([]);
    const created = ENFORCEMENT_CASES[3]; // EN-2205, 'registered'

    const promise = store.register({
      bailiff: created.bailiff,
      caseNumber: created.caseNumber,
      customer: 'CUST-002',
      blockedAmount: created.blockedAmount,
      protectedAmount: created.protectedAmount,
    });
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/enforcement`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.transferredAmount).toBeUndefined();
    req.flush(created);
    await promise;

    expect(store.cases()).toEqual([created]);
  });

  it('requestTransferApproval() posts to /enforcement/{id}/request-transfer-approval and updates the signal', async () => {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/enforcement`).flush(ENFORCEMENT_CASES);
    const item = ENFORCEMENT_CASES[3];
    const updated = { ...item, status: 'active' as const };

    const promise = store.requestTransferApproval(item.id);
    const req = httpMock.expectOne(
      `${apiBaseUrl.bankingBackend}/enforcement/${item.id}/request-transfer-approval`,
    );
    expect(req.request.method).toBe('POST');
    req.flush({ enforcementCase: updated, approvalId: 'appr-1' });
    await promise;

    expect(store.cases().find((c) => c.id === item.id)?.status).toBe('active');
  });

  it('transfer() posts to /enforcement/{id}/transfer and updates the signal', async () => {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/enforcement`).flush(ENFORCEMENT_CASES);
    const item = ENFORCEMENT_CASES[0]; // EN-2201, 'active'
    const updated = { ...item, status: 'settled' as const, transferredAmount: 4800 };

    const promise = store.transfer(item.id, 4800);
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/enforcement/${item.id}/transfer`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ transferredAmount: 4800 });
    req.flush(updated);
    await promise;

    expect(store.cases().find((c) => c.id === item.id)?.status).toBe('settled');
  });
});
