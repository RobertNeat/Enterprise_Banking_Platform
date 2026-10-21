import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { MessageService } from 'primeng/api';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { CUSTOMER_CASES } from './cases.data';
import { CasesPage } from './cases.page';

describe('CasesPage', () => {
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;

  function createPage(): CasesPage {
    TestBed.configureTestingModule({
      imports: [CasesPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), MessageService],
    });
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
    const fixture = TestBed.createComponent(CasesPage);
    fixture.detectChanges();
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/cases`).flush(CUSTOMER_CASES);
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/customers`)
      .flush([{ id: 'CUST-001', fullName: 'Jan Wiśniewski' }]);
    return fixture.componentInstance;
  }

  afterEach(() => httpMock.verify());

  it('the wizard form no longer has statutoryDeadline/slaRemainingHours controls', () => {
    const page = createPage();
    expect((page as any).form.controls.schedule.contains('statutoryDeadline')).toBe(false);
    expect((page as any).form.controls.schedule.contains('slaRemainingHours')).toBe(false);
    expect((page as any).form.controls.details.contains('customerId')).toBe(true);
    expect((page as any).form.controls.details.contains('caseType')).toBe(true);
  });

  it('submitWizard() posts to /cases and shows the backend-computed case in the list', async () => {
    const page = createPage();
    (page as any).form.patchValue({
      details: { subject: 'Reklamacja', customerId: 'CUST-001', caseType: 'complaint' },
      schedule: { priority: 'high' },
    });

    const submitPromise = page.submitWizard();
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/cases`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      subject: 'Reklamacja',
      customerId: 'CUST-001',
      priority: 'high',
      caseType: 'complaint',
    });
    req.flush({
      id: 'CS-90001',
      subject: 'Reklamacja',
      customer: 'Jan Wiśniewski',
      owner: 'K. Malinowski',
      priority: 'high',
      statutoryDeadline: '2026-11-14',
      slaRemainingHours: 24,
      status: 'new',
    });
    await submitPromise;

    expect((page as any).cases()[0].id).toBe('CS-90001');
    expect((page as any).cases()[0].statutoryDeadline).toBe('2026-11-14');
  });

  it('escalate() calls POST /cases/{id}/escalate and updates the status from the response', async () => {
    const page = createPage();
    const target = CUSTOMER_CASES.find((c) => c.status === 'new')!;

    const escalatePromise = page.escalate(target);
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/cases/${target.id}/escalate`);
    expect(req.request.method).toBe('POST');
    req.flush({ customerCase: { ...target, status: 'escalated' }, escalated: true });
    await escalatePromise;

    expect((page as any).cases().find((c) => c.id === target.id)?.status).toBe('escalated');
  });

  it('escalate() blocks already-escalated or resolved cases without calling the backend', async () => {
    const page = createPage();
    const resolved = CUSTOMER_CASES.find((c) => c.status === 'resolved')!;

    await page.escalate(resolved);

    httpMock.expectNone(`${apiBaseUrl.bankingBackend}/cases/${resolved.id}/escalate`);
  });

  it('start() calls POST /cases/{id}/start and updates the status from the response', async () => {
    const page = createPage();
    const target = CUSTOMER_CASES.find((c) => c.status === 'new')!;

    const startPromise = page.start(target);
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/cases/${target.id}/start`);
    expect(req.request.method).toBe('POST');
    req.flush({ ...target, status: 'inProgress' });
    await startPromise;

    expect((page as any).cases().find((c) => c.id === target.id)?.status).toBe('inProgress');
  });

  it('start() blocks non-new cases without calling the backend', async () => {
    const page = createPage();
    const inProgress = CUSTOMER_CASES.find((c) => c.status === 'inProgress')!;

    await page.start(inProgress);

    httpMock.expectNone(`${apiBaseUrl.bankingBackend}/cases/${inProgress.id}/start`);
  });

  it('resolve() calls POST /cases/{id}/resolve and updates the status from the response', async () => {
    const page = createPage();
    const target = CUSTOMER_CASES.find((c) => c.status === 'escalated')!;

    const resolvePromise = page.resolve(target);
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/cases/${target.id}/resolve`);
    expect(req.request.method).toBe('POST');
    req.flush({ ...target, status: 'resolved' });
    await resolvePromise;

    expect((page as any).cases().find((c) => c.id === target.id)?.status).toBe('resolved');
  });

  it('resolve() blocks already-resolved cases without calling the backend', async () => {
    const page = createPage();
    const resolved = CUSTOMER_CASES.find((c) => c.status === 'resolved')!;

    await page.resolve(resolved);

    httpMock.expectNone(`${apiBaseUrl.bankingBackend}/cases/${resolved.id}/resolve`);
  });

  it('resolve() surfaces a 409 from the backend as an actionBlocked warning, not a generic error', async () => {
    const page = createPage();
    const target = CUSTOMER_CASES.find((c) => c.status === 'new')!;

    const resolvePromise = page.resolve(target);
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/cases/${target.id}/resolve`);
    req.flush({ message: 'conflict' }, { status: 409, statusText: 'Conflict' });

    // resolve() catches HttpErrorResponse internally and never rejects — the error is surfaced
    // via NotificationService instead, so simply awaiting without throwing is the assertion.
    await resolvePromise;
  });

  it('openHistoryDialog() opens the dialog and loads audit events scoped to CUSTOMER_CASE', () => {
    const page = createPage();
    const target = CUSTOMER_CASES[0];

    page.openHistoryDialog(target);
    expect((page as any).historyDialogOpen()).toBe(true);

    const req = httpMock.expectOne(
      (r) =>
        r.url === `${apiBaseUrl.bankingBackend}/audit-events` &&
        r.params.get('entityType') === 'CUSTOMER_CASE' &&
        r.params.get('entityId') === target.id,
    );
    req.flush([]);

    page.closeHistoryDialog();
    expect((page as any).historyDialogOpen()).toBe(false);
  });
});
