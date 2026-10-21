import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { LOAN_APPLICATIONS } from './loans.data';
import { LoanDetail, SubmitForApprovalResponse } from './loans.model';
import { LoansStore } from './loans.store';

const DETAIL: LoanDetail = {
  ...LOAN_APPLICATIONS[0],
  id: 'LN-90001',
  stage: 'application',
  interestRate: 0.079,
  termMonths: 36,
  installments: [],
  stageTransitions: [],
};

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

  it('create() posts to /loans and prepends the backend-scored result', async () => {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans`).flush([]);

    const promise = store.create({
      customerId: 'CUST-001',
      segment: 'retail',
      product: 'cash',
      requestedAmount: 10000,
    });
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans`);
    expect(req.request.method).toBe('POST');
    req.flush(DETAIL);
    const result = await promise;

    expect(result).toEqual(DETAIL);
    expect(store.applications()[0].id).toBe('LN-90001');
  });

  it('submitForApproval() posts and replaces the loan with the updated stage', async () => {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans`).flush([DETAIL]);

    const response: SubmitForApprovalResponse = {
      loan: { ...DETAIL, stage: 'decision' },
      approvalId: 'appr-1',
    };
    const promise = store.submitForApproval('LN-90001');
    const req = httpMock.expectOne(
      `${apiBaseUrl.bankingBackend}/loans/LN-90001/submit-for-approval`,
    );
    expect(req.request.method).toBe('POST');
    req.flush(response);
    const result = await promise;

    expect(result.approvalId).toBe('appr-1');
    expect(store.findById('LN-90001')?.stage).toBe('decision');
  });

  it('advanceStage() posts and replaces the loan with the contract stage + installments', async () => {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans`).flush([DETAIL]);

    const advanced: LoanDetail = { ...DETAIL, stage: 'contract', approvedAmount: 10000 };
    const promise = store.advanceStage('LN-90001');
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans/LN-90001/advance-stage`);
    expect(req.request.method).toBe('POST');
    req.flush(advanced);
    await promise;

    expect(store.findById('LN-90001')?.stage).toBe('contract');
  });

  it('advanceStage() on a REJECTED decision replaces the loan back at the application stage', async () => {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans`).flush([DETAIL]);

    const reverted: LoanDetail = { ...DETAIL, stage: 'application' };
    const promise = store.advanceStage('LN-90001');
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans/LN-90001/advance-stage`);
    req.flush(reverted);
    await promise;

    expect(store.findById('LN-90001')?.stage).toBe('application');
  });

  it('disburse() posts and replaces the loan with the schedule stage', async () => {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans`).flush([DETAIL]);

    const disbursed: LoanDetail = { ...DETAIL, stage: 'schedule' };
    const promise = store.disburse('LN-90001');
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans/LN-90001/disburse`);
    expect(req.request.method).toBe('POST');
    req.flush(disbursed);
    await promise;

    expect(store.findById('LN-90001')?.stage).toBe('schedule');
  });
});
