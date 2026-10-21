import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { REGULATORY_REPORTS } from './reporting.data';
import { ReportingStore } from './reporting.store';

describe('ReportingStore', () => {
  let store: ReportingStore;
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
    store = TestBed.inject(ReportingStore);
  });

  afterEach(() => httpMock.verify());

  it('fetches /reporting/reports from banking_backend and populates the signal', () => {
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/reporting/reports`);
    expect(req.request.method).toBe('GET');
    req.flush(REGULATORY_REPORTS);

    expect(store.reports()).toEqual(REGULATORY_REPORTS);
  });

  it('submit() posts to /reporting/reports/{id}/submit and updates the signal', async () => {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/reporting/reports`).flush(REGULATORY_REPORTS);
    const report = REGULATORY_REPORTS[0]; // RPT-KNF-001, 'draft'
    const updated = { ...report, status: 'submitted' as const };

    const promise = store.submit(report.id);
    const req = httpMock.expectOne(
      `${apiBaseUrl.bankingBackend}/reporting/reports/${report.id}/submit`,
    );
    expect(req.request.method).toBe('POST');
    req.flush(updated);
    await promise;

    expect(store.reports().find((r) => r.id === report.id)?.status).toBe('submitted');
  });

  it('refreshSubmissionStatus() posts to /reporting/reports/{id}/refresh-submission-status and updates the signal', async () => {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/reporting/reports`).flush(REGULATORY_REPORTS);
    const report = REGULATORY_REPORTS[1]; // RPT-NBP-014, 'submitted'
    const updatedReport = { ...report, status: 'accepted' as const };

    const promise = store.refreshSubmissionStatus(report.id);
    const req = httpMock.expectOne(
      `${apiBaseUrl.bankingBackend}/reporting/reports/${report.id}/refresh-submission-status`,
    );
    expect(req.request.method).toBe('POST');
    req.flush({
      report: updatedReport,
      submission: {
        gatewaySubmissionId: 'sub-1',
        gatewayStatus: 'ACCEPTED',
        validationErrors: null,
        submittedAt: '2026-10-03T11:00:00Z',
        resolvedAt: '2026-10-03T11:05:00Z',
      },
    });
    await promise;

    expect(store.reports().find((r) => r.id === report.id)?.status).toBe('accepted');
  });

  it('findSubmissions() gets /reporting/reports/{id}/submissions', async () => {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/reporting/reports`).flush(REGULATORY_REPORTS);
    const report = REGULATORY_REPORTS[1]; // RPT-NBP-014
    const submissions = [
      {
        gatewaySubmissionId: 'sub-1',
        gatewayStatus: 'ACCEPTED',
        validationErrors: null,
        submittedAt: '2026-10-03T11:00:00Z',
        resolvedAt: '2026-10-03T11:05:00Z',
      },
    ];

    const promise = store.findSubmissions(report.id);
    const req = httpMock.expectOne(
      `${apiBaseUrl.bankingBackend}/reporting/reports/${report.id}/submissions`,
    );
    expect(req.request.method).toBe('GET');
    req.flush(submissions);

    expect(await promise).toEqual(submissions);
  });
});
