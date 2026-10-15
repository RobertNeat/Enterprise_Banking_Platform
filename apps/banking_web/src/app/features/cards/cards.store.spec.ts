import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { CARD_AUTHORIZATIONS, PAYMENT_CARDS } from './cards.data';
import { Chargeback, PaymentCard } from './cards.model';
import { CardsStore } from './cards.store';

const CHARGEBACKS: Chargeback[] = [
  {
    id: 'd9a633a4-1223-48cd-b470-217a69610114',
    cardAuthorizationId: '449084f7-77b5-4cab-a790-e680763a6572',
    reasonCode: '4837',
    status: 'FILED',
    filedAt: '2026-10-03T10:00:00+02:00',
  },
];

describe('CardsStore', () => {
  let store: CardsStore;
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
    store = TestBed.inject(CardsStore);
  });

  afterEach(() => httpMock.verify());

  function flushInitialRequests(): void {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/cards`).flush(PAYMENT_CARDS);
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/cards/authorizations`)
      .flush(CARD_AUTHORIZATIONS);
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/cards/chargebacks`).flush(CHARGEBACKS);
  }

  it('fetches /cards, /cards/authorizations and /cards/chargebacks and populates signals', () => {
    flushInitialRequests();

    expect(store.cards()).toEqual(PAYMENT_CARDS);
    expect(store.authorizations()).toEqual(CARD_AUTHORIZATIONS);
    expect(store.chargebacks()).toEqual(CHARGEBACKS);
  });

  it('blockCard() posts to /cards/{id}/block and replaces the card with the backend response', async () => {
    flushInitialRequests();

    const blocked: PaymentCard = { ...PAYMENT_CARDS[0], status: 'blocked' };
    const promise = store.blockCard('CRD-5521');
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/cards/CRD-5521/block`);
    expect(req.request.method).toBe('POST');
    req.flush(blocked);
    const result = await promise;

    expect(result.status).toBe('blocked');
    expect(store.cards().find((c) => c.id === 'CRD-5521')?.status).toBe('blocked');
  });

  it('unblockCard() posts to /cards/{id}/unblock and replaces the card with the backend response', async () => {
    flushInitialRequests();

    const unblocked: PaymentCard = { ...PAYMENT_CARDS[1], status: 'active' };
    const promise = store.unblockCard('CRD-5522');
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/cards/CRD-5522/unblock`);
    expect(req.request.method).toBe('POST');
    req.flush(unblocked);
    const result = await promise;

    expect(result.status).toBe('active');
    expect(store.cards().find((c) => c.id === 'CRD-5522')?.status).toBe('active');
  });

  it('fileChargeback() posts to /cards/{id}/chargebacks and prepends the created chargeback', async () => {
    flushInitialRequests();

    const created: Chargeback = {
      id: 'new-cb-1',
      cardAuthorizationId: 'dfd92a8d-976a-4601-97be-13ab2e09c7b1',
      reasonCode: '4837',
      status: 'FILED',
      filedAt: '2026-10-15T10:00:00+02:00',
    };
    const promise = store.fileChargeback('CRD-5521', {
      authorizationId: 'dfd92a8d-976a-4601-97be-13ab2e09c7b1',
      reasonCode: '4837',
      description: 'opis',
    });
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/cards/CRD-5521/chargebacks`);
    expect(req.request.method).toBe('POST');
    req.flush(created);
    const result = await promise;

    expect(result.id).toBe('new-cb-1');
    expect(store.chargebacks()[0].id).toBe('new-cb-1');
  });
});
