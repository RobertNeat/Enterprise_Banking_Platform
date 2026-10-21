import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { MessageService } from 'primeng/api';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { NotificationService } from '../../core/services/notification.service';
import { ENFORCEMENT_CASES } from './enforcement.data';
import { EnforcementPage } from './enforcement.page';

/** Pre-seeds sessionStorage the way AuthService itself persists a session (see auth.service.ts). */
function storeSession(employeeId: string, seniorityLevel: number): void {
  sessionStorage.setItem(
    'bank.auth.session',
    JSON.stringify({
      token: 'jwt-token-value',
      employeeId,
      role: 'ignored',
      permissions: ['enforcement'],
      seniorityLevel,
    }),
  );
}

describe('EnforcementPage', () => {
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;
  let lastFixture: ReturnType<typeof TestBed.createComponent<EnforcementPage>>;

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  function createPage(): EnforcementPage {
    TestBed.configureTestingModule({
      imports: [EnforcementPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), MessageService],
    });
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
    const fixture = TestBed.createComponent(EnforcementPage);
    lastFixture = fixture;
    fixture.detectChanges();
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/enforcement`).flush(ENFORCEMENT_CASES);
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/customers`)
      .flush([{ id: 'CUST-001', fullName: 'Jan Wiśniewski' }]);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  it('canRequestTransferApproval is false for admin (seniorityLevel 3, highest in the system) and the row button is hidden', () => {
    storeSession('PRC-00001', 3);
    const page = createPage();
    const registeredId = ENFORCEMENT_CASES.find((c) => c.status === 'registered')!.id;
    expect((page as any).canRequestTransferApproval()).toBe(false);
    expect(
      lastFixture.nativeElement.querySelector(
        `#enforcement-row-${registeredId}-request-approval-btn`,
      ),
    ).toBeNull();
  });

  it('canRequestTransferApproval is true for a non-admin role and the row button is shown', () => {
    storeSession('PRC-30678', 1);
    const page = createPage();
    const registeredId = ENFORCEMENT_CASES.find((c) => c.status === 'registered')!.id;
    expect((page as any).canRequestTransferApproval()).toBe(true);
    expect(
      lastFixture.nativeElement.querySelector(
        `#enforcement-row-${registeredId}-request-approval-btn`,
      ),
    ).not.toBeNull();
  });

  it('requestTransferApproval() maps a 403 to the forbidden toast without throwing', async () => {
    storeSession('PRC-00001', 3);
    const page = createPage();
    const errorSpy = vi.spyOn(TestBed.inject(NotificationService), 'error');
    const item = ENFORCEMENT_CASES.find((c) => c.status === 'registered')!;

    const promise = page.requestTransferApproval(item);
    const req = httpMock.expectOne(
      `${apiBaseUrl.bankingBackend}/enforcement/${item.id}/request-transfer-approval`,
    );
    req.flush({ message: 'forbidden' }, { status: 403, statusText: 'Forbidden' });

    // requestTransferApproval() catches the error internally and shows a toast — it must not
    // reject/throw out to the caller.
    await promise;

    expect(errorSpy).toHaveBeenCalledWith('enforcement.error.forbidden');
  });

  it('openHistoryDialog() opens the dialog and loads audit events scoped to ENFORCEMENT_CASE', () => {
    storeSession('PRC-30678', 1);
    const page = createPage();
    const item = ENFORCEMENT_CASES[0];

    page.openHistoryDialog(item);
    expect((page as any).historyDialogOpen()).toBe(true);

    const req = httpMock.expectOne(
      (r) =>
        r.url === `${apiBaseUrl.bankingBackend}/audit-events` &&
        r.params.get('entityType') === 'ENFORCEMENT_CASE' &&
        r.params.get('entityId') === item.id,
    );
    req.flush([]);

    page.closeHistoryDialog();
    expect((page as any).historyDialogOpen()).toBe(false);
  });
});
