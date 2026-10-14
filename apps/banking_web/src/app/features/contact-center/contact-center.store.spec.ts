import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { CONTACT_HISTORY, CUSTOMER_PROFILE, DISPOSITIONS } from './contact-center.data';
import { ContactCenterStore } from './contact-center.store';

describe('ContactCenterStore', () => {
  let store: ContactCenterStore;
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
    store = TestBed.inject(ContactCenterStore);
  });

  afterEach(() => httpMock.verify());

  it('fetches dispositions, history and the default customer profile', () => {
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/contact-center/dispositions`)
      .flush(DISPOSITIONS);
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/contact-center/history`)
      .flush(CONTACT_HISTORY);
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/contact-center/profile/CUST-001`)
      .flush(CUSTOMER_PROFILE);

    expect(store.dispositions()).toEqual(DISPOSITIONS);
    expect(store.history()).toEqual(CONTACT_HISTORY);
    expect(store.profile()).toEqual(CUSTOMER_PROFILE);
  });
});
