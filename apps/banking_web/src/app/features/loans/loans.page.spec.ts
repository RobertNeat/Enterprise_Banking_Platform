import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { LOAN_APPLICATIONS } from './loans.data';
import { LoansPage } from './loans.page';

describe('LoansPage', () => {
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;

  function createPage(): LoansPage {
    TestBed.configureTestingModule({
      imports: [LoansPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        MessageService,
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
    const fixture = TestBed.createComponent(LoansPage);
    fixture.detectChanges();
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans`).flush(LOAN_APPLICATIONS);
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/customers`)
      .flush([{ id: 'CUST-001', fullName: 'Jan Wiśniewski', segment: 'retail' }]);
    return fixture.componentInstance;
  }

  afterEach(() => httpMock.verify());

  it('the wizard form no longer has scoreResult/dsti controls', () => {
    const page = createPage();
    expect((page as any).form.contains('scoreResult')).toBe(false);
    expect((page as any).form.contains('dsti')).toBe(false);
    expect((page as any).form.contains('customerId')).toBe(true);
  });

  it('entering the scoring step (2) posts to /loans and stores the backend-scored result', async () => {
    const page = createPage();
    (page as any).form.patchValue({
      customerId: 'CUST-001',
      segment: 'retail',
      product: 'cash',
      requestedAmount: 10000,
    });

    const stepPromise = page.onWizardStepChange(2);
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      customerId: 'CUST-001',
      segment: 'retail',
      product: 'cash',
      requestedAmount: 10000,
    });
    req.flush({
      ...LOAN_APPLICATIONS[0],
      id: 'LN-90001',
      scoreResult: 710,
      dsti: 0.33,
      stage: 'application',
      interestRate: 0.079,
      termMonths: 36,
      installments: [],
      stageTransitions: [],
    });
    await stepPromise;

    expect((page as any).createdLoan().scoreResult).toBe(710);
    expect((page as any).scoringError()).toBe(false);
  });

  it('submitWizard() calls submit-for-approval for the created loan', async () => {
    const page = createPage();
    (page as any).createdLoan.set({
      ...LOAN_APPLICATIONS[0],
      id: 'LN-90001',
      stage: 'application',
      interestRate: 0.079,
      termMonths: 36,
      installments: [],
      stageTransitions: [],
    });

    const submitPromise = page.submitWizard();
    const req = httpMock.expectOne(
      `${apiBaseUrl.bankingBackend}/loans/LN-90001/submit-for-approval`,
    );
    expect(req.request.method).toBe('POST');
    req.flush({
      loan: { ...LOAN_APPLICATIONS[0], id: 'LN-90001', stage: 'decision' },
      approvalId: 'appr-1',
    });
    await submitPromise;

    expect((page as any).wizardOpen()).toBe(false);
  });
});
