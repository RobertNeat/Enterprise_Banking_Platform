import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { NotificationService } from '../../core/services/notification.service';
import { REGULATORY_REPORTS } from './reporting.data';
import { ReportingPage } from './reporting.page';

describe('ReportingPage', () => {
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;

  function createPage(): ReportingPage {
    TestBed.configureTestingModule({
      imports: [ReportingPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        MessageService,
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
    const fixture = TestBed.createComponent(ReportingPage);
    fixture.detectChanges();
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/reporting/reports`).flush(REGULATORY_REPORTS);
    return fixture.componentInstance;
  }

  afterEach(() => httpMock.verify());

  it('openSubmissionsDialog() opens the dialog and loads submissions for the report, including validationErrors', async () => {
    const page = createPage();
    const report = REGULATORY_REPORTS[1]; // RPT-NBP-014

    const openPromise = page.openSubmissionsDialog(report);
    expect(page['submissionsDialogOpen']()).toBe(true);
    expect(page['submissionsLoading']()).toBe(true);

    const req = httpMock.expectOne(
      `${apiBaseUrl.bankingBackend}/reporting/reports/${report.id}/submissions`,
    );
    expect(req.request.method).toBe('GET');
    const submissions = [
      {
        gatewaySubmissionId: 'sub-1',
        gatewayStatus: 'REJECTED',
        validationErrors: 'Missing field: totalExposure',
        submittedAt: '2026-10-01T09:00:00Z',
        resolvedAt: '2026-10-01T09:05:00Z',
      },
      {
        gatewaySubmissionId: 'sub-2',
        gatewayStatus: 'ACCEPTED',
        validationErrors: null,
        submittedAt: '2026-10-03T11:00:00Z',
        resolvedAt: '2026-10-03T11:05:00Z',
      },
    ];
    req.flush(submissions);
    await openPromise;

    expect(page['submissionsLoading']()).toBe(false);
    expect(page['submissions']()).toEqual(submissions);

    page.closeSubmissionsDialog();
    expect(page['submissionsDialogOpen']()).toBe(false);
    expect(page['submissions']()).toEqual([]);
  });

  it('openSubmissionsDialog() toasts an error and clears the list when the request fails', async () => {
    const page = createPage();
    const errorSpy = vi.spyOn(TestBed.inject(NotificationService), 'error');
    const report = REGULATORY_REPORTS[1];

    const openPromise = page.openSubmissionsDialog(report);
    const req = httpMock.expectOne(
      `${apiBaseUrl.bankingBackend}/reporting/reports/${report.id}/submissions`,
    );
    req.flush({ message: 'unavailable' }, { status: 503, statusText: 'Service Unavailable' });

    await openPromise;

    expect(page['submissions']()).toEqual([]);
    expect(errorSpy).toHaveBeenCalledWith('reporting.submissionsHistory.error');
  });
});
