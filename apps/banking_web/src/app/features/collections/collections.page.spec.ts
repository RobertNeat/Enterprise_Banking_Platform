import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { MessageService } from 'primeng/api';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { NotificationService } from '../../core/services/notification.service';
import { COLLECTIONS_CASES } from './collections.data';
import { CollectionsPage } from './collections.page';

describe('CollectionsPage', () => {
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;

  function createPage(): CollectionsPage {
    TestBed.configureTestingModule({
      imports: [CollectionsPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), MessageService],
    });
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
    const fixture = TestBed.createComponent(CollectionsPage);
    fixture.detectChanges();
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/collections`).flush(COLLECTIONS_CASES);
    return fixture.componentInstance;
  }

  afterEach(() => httpMock.verify());

  it('openActionsDialog() opens the dialog and loads actions scoped to the case id', async () => {
    const page = createPage();
    const item = COLLECTIONS_CASES[0]; // DBT-6601

    const openPromise = page.openActionsDialog(item);
    expect(page['actionsDialogOpen']()).toBe(true);
    expect(page['actionsLoading']()).toBe(true);

    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/collections/${item.id}/actions`);
    expect(req.request.method).toBe('GET');
    const actions = [
      {
        actionType: 'REMINDER_SENT',
        performedAt: '2026-07-15T10:00:00Z',
        performedBy: 'PRC-30678',
        governanceAuditEventId: null,
      },
      {
        actionType: 'SETTLEMENT_PROPOSED',
        performedAt: '2026-08-21T10:00:00Z',
        performedBy: 'PRC-30678',
        governanceAuditEventId: null,
      },
    ];
    req.flush(actions);
    await openPromise;

    expect(page['actionsLoading']()).toBe(false);
    expect(page['actions']()).toEqual(actions);

    page.closeActionsDialog();
    expect(page['actionsDialogOpen']()).toBe(false);
    expect(page['actions']()).toEqual([]);
  });

  it('openActionsDialog() toasts an error and clears the list when the request fails', async () => {
    const page = createPage();
    const errorSpy = vi.spyOn(TestBed.inject(NotificationService), 'error');
    const item = COLLECTIONS_CASES[0];

    const openPromise = page.openActionsDialog(item);
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/collections/${item.id}/actions`);
    req.flush({ message: 'unavailable' }, { status: 503, statusText: 'Service Unavailable' });

    await openPromise;

    expect(page['actions']()).toEqual([]);
    expect(errorSpy).toHaveBeenCalledWith('collections.actionsHistory.error');
  });
});
