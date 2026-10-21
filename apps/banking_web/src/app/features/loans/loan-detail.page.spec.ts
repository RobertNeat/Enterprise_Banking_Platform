import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { LOAN_APPLICATIONS } from './loans.data';
import { LoanDetailPage } from './loan-detail.page';

describe('LoanDetailPage', () => {
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;

  function configure(loanId: string) {
    TestBed.configureTestingModule({
      imports: [LoanDetailPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        MessageService,
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: loanId }) } },
        },
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
  }

  afterEach(() => httpMock.verify());

  it('always fetches /loans/{id} so it gets the real installments, not just on a deep link', () => {
    configure('LN-80231');

    const fixture = TestBed.createComponent(LoanDetailPage);
    const component = fixture.componentInstance;

    // LoansStore itself also loads its own list in the background.
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans`).flush(LOAN_APPLICATIONS);

    const seededInstallments = [
      {
        no: 1,
        dueDate: '2026-10-12',
        principal: 1916.67,
        interest: 3028.33,
        status: 'upcoming' as const,
      },
    ];
    const detailReq = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans/LN-80231`);
    expect(detailReq.request.method).toBe('GET');
    detailReq.flush({
      ...LOAN_APPLICATIONS[0],
      installments: seededInstallments,
      stageTransitions: [],
    });

    expect((component as any).loan().id).toBe('LN-80231');
    expect((component as any).schedule()).toEqual(seededInstallments);
  });

  it('shows an empty schedule (not a synthetic one) when the backend returns no installments yet', () => {
    configure('LN-80288');

    const fixture = TestBed.createComponent(LoanDetailPage);
    const component = fixture.componentInstance;

    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans`).flush(LOAN_APPLICATIONS);
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans/LN-80288`).flush({
      ...LOAN_APPLICATIONS.find((l) => l.id === 'LN-80288'),
      installments: [],
      stageTransitions: [],
    });

    expect((component as any).schedule()).toEqual([]);
  });

  it('builds the locally-generated schedule from the loan own interestRate/termMonths, not hardcoded constants', () => {
    // Deep link to an id the store doesn't carry, so loan()/fetchedLoan() resolve from the
    // fetched LoanDetail (with its own interestRate/termMonths) rather than the store's list.
    configure('LN-90001');

    const fixture = TestBed.createComponent(LoanDetailPage);
    const component = fixture.componentInstance;

    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans`).flush(LOAN_APPLICATIONS);
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans/LN-90001`).flush({
      ...LOAN_APPLICATIONS.find((l) => l.id === 'LN-80288'),
      id: 'LN-90001',
      requestedAmount: 120000,
      approvedAmount: 120000,
      interestRate: 0.12,
      termMonths: 24,
      installments: null,
      stageTransitions: [],
    });

    const schedule = (component as any).schedule();
    expect(schedule.length).toBe(24);
    // monthlyPrincipal = 120000 / 24 = 5000; first interest = 120000 * (0.12 / 12) = 1200,
    // which differs from the old hardcoded 0.079/12 defaults (would give 790).
    expect(schedule[0].principal).toBe(5000);
    expect(schedule[0].interest).toBe(1200);
  });

  it('falls back to EMPTY_LOAN / locally-generated schedule when the backend has no such loan', () => {
    configure('LN-99999');

    const fixture = TestBed.createComponent(LoanDetailPage);
    const component = fixture.componentInstance;

    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans`).flush([]);
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/loans/LN-99999`)
      .flush({ message: 'not found' }, { status: 404, statusText: 'Not Found' });

    expect((component as any).loan().id).toBe('');
  });

  it('openHistoryDialog() opens the dialog and loads audit events scoped to LOAN_APPLICATION', () => {
    configure('LN-80231');

    const fixture = TestBed.createComponent(LoanDetailPage);
    const component = fixture.componentInstance;

    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans`).flush(LOAN_APPLICATIONS);
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans/LN-80231`).flush({
      ...LOAN_APPLICATIONS[0],
      installments: [],
      stageTransitions: [],
    });

    component.openHistoryDialog();
    expect((component as any).historyDialogOpen()).toBe(true);

    const req = httpMock.expectOne(
      (r) =>
        r.url === `${apiBaseUrl.bankingBackend}/audit-events` &&
        r.params.get('entityType') === 'LOAN_APPLICATION' &&
        r.params.get('entityId') === 'LN-80231',
    );
    req.flush([]);

    component.closeHistoryDialog();
    expect((component as any).historyDialogOpen()).toBe(false);
  });

  it('renders stageTransitions sorted by transitionedAt, even when loan() comes from the store', () => {
    configure('LN-80231');

    const fixture = TestBed.createComponent(LoanDetailPage);
    const component = fixture.componentInstance;

    // LoansStore's /loans response makes loan() resolve from the store (plain LoanApplication,
    // no stageTransitions) — the timeline must still come from the separate /loans/{id} fetch.
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans`).flush(LOAN_APPLICATIONS);

    const transitions = [
      {
        fromStage: 'decision',
        toStage: 'contract',
        transitionedAt: '2026-09-10T09:00:00Z',
        transitionedBy: 'PRC-30678',
        approvalId: 'APR-1002',
      },
      {
        fromStage: 'application',
        toStage: 'decision',
        transitionedAt: '2026-09-01T09:00:00Z',
        transitionedBy: 'PRC-30678',
        approvalId: null,
      },
    ];
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans/LN-80231`).flush({
      ...LOAN_APPLICATIONS[0],
      installments: [],
      stageTransitions: transitions,
    });

    expect((component as any).stageTransitions()).toEqual([transitions[1], transitions[0]]);
  });

  it('refreshes stageTransitions after advanceStage()', async () => {
    // LN-80255 (Agro-Bud) is seeded in stage 'decision' — canAdvance() requires that stage, and
    // loan() prefers LoansStore's own (unmodified) record, so the stage override has to live there.
    configure('LN-80255');

    const fixture = TestBed.createComponent(LoanDetailPage);
    const component = fixture.componentInstance;
    const seeded = LOAN_APPLICATIONS.find((l) => l.id === 'LN-80255')!;

    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans`).flush(LOAN_APPLICATIONS);
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans/LN-80255`).flush({
      ...seeded,
      installments: [],
      stageTransitions: [],
    });

    const advancePromise = component.advanceStage();
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans/LN-80255/advance-stage`);
    const newTransition = {
      fromStage: 'decision',
      toStage: 'contract',
      transitionedAt: '2026-10-20T09:00:00Z',
      transitionedBy: 'PRC-30678',
      approvalId: 'APR-1003',
    };
    req.flush({
      ...seeded,
      stage: 'contract',
      installments: [],
      stageTransitions: [newTransition],
    });
    await advancePromise;

    expect((component as any).stageTransitions()).toEqual([newTransition]);
  });

  it('advanceStage() on a REJECTED decision returns the loan to application stage without erroring', async () => {
    // LN-80255 is seeded in stage 'decision' — canAdvance() requires that stage.
    configure('LN-80255');

    const fixture = TestBed.createComponent(LoanDetailPage);
    const component = fixture.componentInstance;
    const seeded = LOAN_APPLICATIONS.find((l) => l.id === 'LN-80255')!;

    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans`).flush(LOAN_APPLICATIONS);
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans/LN-80255`).flush({
      ...seeded,
      installments: [],
      stageTransitions: [],
    });

    const advancePromise = component.advanceStage();
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans/LN-80255/advance-stage`);
    req.flush({
      ...seeded,
      stage: 'application',
      installments: [],
      stageTransitions: [
        {
          fromStage: 'decision',
          toStage: 'application',
          transitionedAt: '2026-10-21T09:00:00Z',
          transitionedBy: 'PRC-30678',
          approvalId: 'APR-1004',
        },
      ],
    });
    await advancePromise;

    expect((component as any).loan().stage).toBe('application');
    expect((component as any).canSubmitForApproval()).toBe(true);
  });

  it('disburse() posts to /disburse and updates the loan to schedule stage', async () => {
    // LN-80231 is seeded in stage 'contract' — canDisburse() requires that stage.
    configure('LN-80231');

    const fixture = TestBed.createComponent(LoanDetailPage);
    const component = fixture.componentInstance;

    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans`).flush(LOAN_APPLICATIONS);
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans/LN-80231`).flush({
      ...LOAN_APPLICATIONS[0],
      installments: [],
      stageTransitions: [],
    });

    const disbursePromise = component.disburse();
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans/LN-80231/disburse`);
    const transitions = [
      {
        fromStage: 'contract',
        toStage: 'disbursement',
        transitionedAt: '2026-10-21T09:00:00.000Z',
        transitionedBy: 'PRC-30678',
        approvalId: null,
      },
      {
        fromStage: 'disbursement',
        toStage: 'schedule',
        transitionedAt: '2026-10-21T09:00:00.001Z',
        transitionedBy: 'PRC-30678',
        approvalId: null,
      },
    ];
    req.flush({
      ...LOAN_APPLICATIONS[0],
      stage: 'schedule',
      installments: [],
      stageTransitions: transitions,
    });
    await disbursePromise;

    expect((component as any).loan().stage).toBe('schedule');
    expect((component as any).stageTransitions()).toEqual(transitions);
  });
});
