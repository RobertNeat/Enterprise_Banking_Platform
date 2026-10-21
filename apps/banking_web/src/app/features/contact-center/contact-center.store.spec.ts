import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { CONTACT_HISTORY, CUSTOMER_PROFILE, CUSTOMERS, DISPOSITIONS } from './contact-center.data';
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

  /** KKT — selektor klienta zasilany GET /customers; pierwszy klient wybrany automatycznie. */
  it('loads customers and auto-selects the first one, fetching their dispositions/history/profile', () => {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/customers`).flush(CUSTOMERS);

    expect(store.customers()).toEqual(CUSTOMERS);
    expect(store.selectedCustomerId()).toBe('CUST-001');

    httpMock
      .expectOne(
        (req) =>
          req.url === `${apiBaseUrl.bankingBackend}/contact-center/dispositions` &&
          req.params.get('customerId') === 'CUST-001',
      )
      .flush(DISPOSITIONS);
    httpMock
      .expectOne(
        (req) =>
          req.url === `${apiBaseUrl.bankingBackend}/contact-center/history` &&
          req.params.get('customerId') === 'CUST-001',
      )
      .flush(CONTACT_HISTORY);
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/contact-center/profile/CUST-001`)
      .flush(CUSTOMER_PROFILE);

    expect(store.dispositions()).toEqual(DISPOSITIONS);
    expect(store.history()).toEqual(CONTACT_HISTORY);
    expect(store.profile()).toEqual(CUSTOMER_PROFILE);
  });

  /** KKT — zmiana klienta przeładowuje dyspozycje/historię/profil parametrem customerId. */
  it('reloads dispositions, history and profile when selecting a different customer', () => {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/customers`).flush(CUSTOMERS);
    httpMock
      .expectOne((req) => req.url === `${apiBaseUrl.bankingBackend}/contact-center/dispositions`)
      .flush(DISPOSITIONS);
    httpMock
      .expectOne((req) => req.url === `${apiBaseUrl.bankingBackend}/contact-center/history`)
      .flush(CONTACT_HISTORY);
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/contact-center/profile/CUST-001`)
      .flush(CUSTOMER_PROFILE);

    store.selectCustomer('CUST-002');
    expect(store.selectedCustomerId()).toBe('CUST-002');

    httpMock
      .expectOne(
        (req) =>
          req.url === `${apiBaseUrl.bankingBackend}/contact-center/dispositions` &&
          req.params.get('customerId') === 'CUST-002',
      )
      .flush([]);
    httpMock
      .expectOne(
        (req) =>
          req.url === `${apiBaseUrl.bankingBackend}/contact-center/history` &&
          req.params.get('customerId') === 'CUST-002',
      )
      .flush([]);
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/contact-center/profile/CUST-002`)
      .flush({ ...CUSTOMER_PROFILE, name: 'Katarzyna Zając' });

    expect(store.dispositions()).toEqual([]);
    expect(store.history()).toEqual([]);
    expect(store.profile()?.name).toBe('Katarzyna Zając');
  });

  /** KKT/F8 — PUT /contact-center/dispositions/{id}/case łączy dyspozycję ze sprawą. */
  it('links a disposition to a case and updates it in place', async () => {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/customers`).flush(CUSTOMERS);
    httpMock
      .expectOne((req) => req.url === `${apiBaseUrl.bankingBackend}/contact-center/dispositions`)
      .flush(DISPOSITIONS);
    httpMock
      .expectOne((req) => req.url === `${apiBaseUrl.bankingBackend}/contact-center/history`)
      .flush(CONTACT_HISTORY);
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/contact-center/profile/CUST-001`)
      .flush(CUSTOMER_PROFILE);

    const updated = { ...DISPOSITIONS[0], linkedCase: 'CS-99999' };
    const linkPromise = store.linkCase('DSP-771', 'CS-99999');

    const req = httpMock.expectOne(
      `${apiBaseUrl.bankingBackend}/contact-center/dispositions/DSP-771/case`,
    );
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ caseId: 'CS-99999' });
    req.flush(updated);

    await linkPromise;

    expect(store.dispositions().find((d) => d.id === 'DSP-771')?.linkedCase).toBe('CS-99999');
  });
});
