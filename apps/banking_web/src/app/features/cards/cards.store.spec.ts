import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { CARD_AUTHORIZATIONS, PAYMENT_CARDS } from './cards.data';
import { CardsStore } from './cards.store';

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

  it('fetches /cards and /cards/authorizations and populates both signals', () => {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/cards`).flush(PAYMENT_CARDS);
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/cards/authorizations`)
      .flush(CARD_AUTHORIZATIONS);

    expect(store.cards()).toEqual(PAYMENT_CARDS);
    expect(store.authorizations()).toEqual(CARD_AUTHORIZATIONS);
  });
});
