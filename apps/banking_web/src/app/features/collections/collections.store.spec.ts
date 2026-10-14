import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { COLLECTIONS_CASES } from './collections.data';
import { CollectionsStore } from './collections.store';

describe('CollectionsStore', () => {
  let store: CollectionsStore;
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
    store = TestBed.inject(CollectionsStore);
  });

  afterEach(() => httpMock.verify());

  it('fetches /collections from banking_backend and populates the signal', () => {
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/collections`);
    expect(req.request.method).toBe('GET');
    req.flush(COLLECTIONS_CASES);

    expect(store.cases()).toEqual(COLLECTIONS_CASES);
  });
});
