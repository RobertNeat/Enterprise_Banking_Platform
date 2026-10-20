import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { NotificationService } from '../../core/services/notification.service';
import { LOAN_APPLICATIONS } from './loans.data';
import { LoansPage } from './loans.page';

/** Pre-seeds sessionStorage the way AuthService itself persists a session (see auth.service.ts). */
function storeSession(employeeId: string, seniorityLevel: number): void {
  sessionStorage.setItem(
    'bank.auth.session',
    JSON.stringify({
      token: 'jwt-token-value',
      employeeId,
      role: 'ignored',
      permissions: ['loans'],
      seniorityLevel,
    }),
  );
}

describe('LoansPage', () => {
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;
  let lastFixture: ReturnType<typeof TestBed.createComponent<LoansPage>>;

  afterEach(() => sessionStorage.clear());

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
    lastFixture = fixture;
    fixture.detectChanges();
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans`).flush(LOAN_APPLICATIONS);
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/customers`)
      .flush([{ id: 'CUST-001', fullName: 'Jan Wiśniewski', segment: 'retail' }]);
    fixture.detectChanges();
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

  it('canCreateLoan is false for admin (seniorityLevel 3, highest in the system) and the Nowy button is hidden', () => {
    storeSession('PRC-00001', 3);
    const page = createPage();
    expect((page as any).canCreateLoan()).toBe(false);
    expect(lastFixture.nativeElement.querySelector('#loans-new-item-btn')).toBeNull();
  });

  it('canCreateLoan is true for a non-admin role and the Nowy button is shown', () => {
    storeSession('PRC-10234', 1);
    const page = createPage();
    expect((page as any).canCreateLoan()).toBe(true);
    expect(lastFixture.nativeElement.querySelector('#loans-new-item-btn')).not.toBeNull();
  });

  it('submitWizard() maps a 403 from submit-for-approval to the forbidden toast', async () => {
    storeSession('PRC-00001', 3);
    const page = createPage();
    const errorSpy = vi.spyOn(TestBed.inject(NotificationService), 'error');
    (page as any).createdLoan.set({
      ...LOAN_APPLICATIONS[0],
      id: 'LN-90002',
      stage: 'application',
      interestRate: 0.079,
      termMonths: 36,
      installments: [],
      stageTransitions: [],
    });

    const submitPromise = page.submitWizard();
    const req = httpMock.expectOne(
      `${apiBaseUrl.bankingBackend}/loans/LN-90002/submit-for-approval`,
    );
    req.flush({ message: 'forbidden' }, { status: 403, statusText: 'Forbidden' });
    await submitPromise;

    expect(errorSpy).toHaveBeenCalledWith('loans.error.forbidden');
  });
});
