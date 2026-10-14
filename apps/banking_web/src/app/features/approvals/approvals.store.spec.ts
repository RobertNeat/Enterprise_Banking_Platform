import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { Approval } from './approvals.model';
import { ApprovalsStore } from './approvals.store';

const APPROVALS: Approval[] = [
  {
    id: 'appr-1',
    type: 'LOAN_STAGE_TRANSITION',
    requestedBy: 'PRC-20456',
    payload: 'LN-80255: scoring -> decision',
    status: 'PENDING',
    createdAt: '2026-10-01T09:00:00Z',
    decidedBy: null,
    decidedAt: null,
    requestedByRole: 'Analityk ryzyka',
    requestedBySeniorityLevel: 1,
  },
];

describe('ApprovalsStore', () => {
  let store: ApprovalsStore;
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
    store = TestBed.inject(ApprovalsStore);
  });

  afterEach(() => httpMock.verify());

  it('load() fetches /approvals from banking_backend without a status filter', async () => {
    const promise = store.load();
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/approvals`);
    expect(req.request.method).toBe('GET');
    req.flush(APPROVALS);
    await promise;

    expect(store.approvals()).toEqual(APPROVALS);
  });

  it('load(status) passes the status as a query param', async () => {
    const promise = store.load('PENDING');
    const req = httpMock.expectOne(
      (r) =>
        r.url === `${apiBaseUrl.bankingBackend}/approvals` && r.params.get('status') === 'PENDING',
    );
    req.flush(APPROVALS);
    await promise;

    expect(store.approvals()).toEqual(APPROVALS);
  });

  it('decide() posts the decision and replaces the item in place', async () => {
    const loadPromise = store.load();
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/approvals`).flush(APPROVALS);
    await loadPromise;

    const decided: Approval = { ...APPROVALS[0], status: 'APPROVED', decidedBy: 'PRC-00001' };
    const decidePromise = store.decide('appr-1', 'APPROVED');
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/approvals/appr-1/decide`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ decision: 'APPROVED' });
    req.flush(decided);
    await decidePromise;

    expect(store.approvals()[0].status).toBe('APPROVED');
    expect(store.approvals()[0].decidedBy).toBe('PRC-00001');
  });
});
