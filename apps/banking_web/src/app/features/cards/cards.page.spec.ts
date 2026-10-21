import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { MessageService } from 'primeng/api';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { CARD_AUTHORIZATIONS, PAYMENT_CARDS } from './cards.data';
import { CardsPage } from './cards.page';

describe('CardsPage', () => {
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;

  function createPage(): CardsPage {
    TestBed.configureTestingModule({
      imports: [CardsPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), MessageService],
    });
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
    const fixture = TestBed.createComponent(CardsPage);
    fixture.detectChanges();
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/cards`).flush(PAYMENT_CARDS);
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/cards/authorizations`)
      .flush(CARD_AUTHORIZATIONS);
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/cards/chargebacks`).flush([]);
    return fixture.componentInstance;
  }

  afterEach(() => httpMock.verify());

  it('toggleBlock() on an active card posts to /block and updates the signal', async () => {
    const page = createPage();
    const card = PAYMENT_CARDS.find((c) => c.id === 'CRD-5521')!;

    const promise = page.toggleBlock(card);
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/cards/CRD-5521/block`);
    expect(req.request.method).toBe('POST');
    req.flush({ ...card, status: 'blocked' });
    await promise;

    expect(page['cards']().find((c) => c.id === 'CRD-5521')?.status).toBe('blocked');
  });

  it('toggleBlock() on a pending card is a no-op (UX guard before any HTTP call)', async () => {
    const page = createPage();
    const card = PAYMENT_CARDS.find((c) => c.id === 'CRD-5541')!;

    await page.toggleBlock(card);

    httpMock.expectNone(`${apiBaseUrl.bankingBackend}/cards/CRD-5541/block`);
    httpMock.expectNone(`${apiBaseUrl.bankingBackend}/cards/CRD-5541/unblock`);
  });

  it('fileChargeback() opens the dialog scoped to the clicked card', () => {
    const page = createPage();
    const card = PAYMENT_CARDS.find((c) => c.id === 'CRD-5521')!;

    page.fileChargeback(card);

    expect(page['chargebackDialogOpen']()).toBe(true);
    expect(page['chargebackCard']()).toEqual(card);
  });

  it('submitChargeback() posts to /cards/{id}/chargebacks and closes the dialog', async () => {
    const page = createPage();
    const card = PAYMENT_CARDS.find((c) => c.id === 'CRD-5521')!;
    page.fileChargeback(card);
    page['chargebackForm'].patchValue({
      authorizationId: 'dfd92a8d-976a-4601-97be-13ab2e09c7b1',
      reasonCode: '4837',
      description: 'opis',
    });

    const promise = page.submitChargeback();
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/cards/CRD-5521/chargebacks`);
    expect(req.request.method).toBe('POST');
    req.flush({
      id: 'new-cb-1',
      cardAuthorizationId: 'dfd92a8d-976a-4601-97be-13ab2e09c7b1',
      reasonCode: '4837',
      status: 'FILED',
      filedAt: '2026-10-15T10:00:00Z',
    });
    await promise;

    expect(page['chargebackDialogOpen']()).toBe(false);
  });

  it('openHistoryDialog() opens the dialog and loads audit events scoped to PAYMENT_CARD', () => {
    const page = createPage();
    const card = PAYMENT_CARDS.find((c) => c.id === 'CRD-5521')!;

    page.openHistoryDialog(card);
    expect(page['historyDialogOpen']()).toBe(true);

    const req = httpMock.expectOne(
      (r) =>
        r.url === `${apiBaseUrl.bankingBackend}/audit-events` &&
        r.params.get('entityType') === 'PAYMENT_CARD' &&
        r.params.get('entityId') === 'CRD-5521',
    );
    req.flush([]);

    page.closeHistoryDialog();
    expect(page['historyDialogOpen']()).toBe(false);
  });
});
