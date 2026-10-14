import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { MessageService } from 'primeng/api';
import { Approval } from './approvals.model';
import { ApprovalsPage } from './approvals.page';

/** Pre-seeds sessionStorage the way AuthService itself persists a session (see auth.service.ts). */
function storeSession(employeeId: string, seniorityLevel: number): void {
  sessionStorage.setItem(
    'bank.auth.session',
    JSON.stringify({
      token: 'jwt-token-value',
      employeeId,
      role: 'ignored',
      permissions: ['approvals'],
      seniorityLevel,
    }),
  );
}

const PENDING_FROM_LOANS: Approval = {
  id: 'appr-1',
  type: 'LOAN_STAGE_TRANSITION',
  requestedBy: 'PRC-10234',
  payload: 'LN-80255',
  status: 'PENDING',
  createdAt: '2026-10-01T09:00:00Z',
  decidedBy: null,
  decidedAt: null,
  requestedByRole: 'Doradca kredytowy',
  requestedBySeniorityLevel: 1,
};

describe('ApprovalsPage', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      imports: [ApprovalsPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), MessageService],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  function createPage(): ApprovalsPage {
    const fixture = TestBed.createComponent(ApprovalsPage);
    fixture.detectChanges();
    httpMock.expectOne((r) => r.url.endsWith('/approvals')).flush([]);
    return fixture.componentInstance;
  }

  it('hides decide/reject for a checker with equal seniority to the requester', () => {
    storeSession('PRC-30678', 1); // collections, seniorityLevel 1 — same as loans (requester)
    const page = createPage();
    expect(page.canDecide(PENDING_FROM_LOANS)).toBe(false);
  });

  it('shows decide/reject for a checker with strictly higher seniority', () => {
    storeSession('PRC-00001', 3); // admin
    const page = createPage();
    expect(page.canDecide(PENDING_FROM_LOANS)).toBe(true);
  });

  it('hides decide/reject when the checker is the requester, even with higher seniority', () => {
    storeSession('PRC-10234', 3); // same id as requestedBy, pretend elevated seniority
    const page = createPage();
    expect(page.canDecide(PENDING_FROM_LOANS)).toBe(false);
  });

  it('hides decide/reject for an already-decided approval', () => {
    storeSession('PRC-00001', 3);
    const page = createPage();
    expect(page.canDecide({ ...PENDING_FROM_LOANS, status: 'APPROVED' })).toBe(false);
  });
});
