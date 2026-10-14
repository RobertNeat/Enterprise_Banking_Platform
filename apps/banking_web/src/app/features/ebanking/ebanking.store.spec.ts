import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { EBANKING_USERS } from './ebanking.data';
import { EbankingStore } from './ebanking.store';

describe('EbankingStore', () => {
  let store: EbankingStore;
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
    store = TestBed.inject(EbankingStore);
  });

  afterEach(() => httpMock.verify());

  it('fetches /ebanking/users from banking_backend and populates the signal', () => {
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/ebanking/users`);
    expect(req.request.method).toBe('GET');
    req.flush(EBANKING_USERS);

    expect(store.users()).toEqual(EBANKING_USERS);
  });
});
