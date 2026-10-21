import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { MessageService } from 'primeng/api';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { CONTACT_HISTORY, CUSTOMER_PROFILE, CUSTOMERS, DISPOSITIONS } from './contact-center.data';
import { ContactCenterPage } from './contact-center.page';

describe('ContactCenterPage', () => {
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;

  function createPage(): ContactCenterPage {
    TestBed.configureTestingModule({
      imports: [ContactCenterPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), MessageService],
    });
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
    const fixture = TestBed.createComponent(ContactCenterPage);
    fixture.detectChanges();

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

    return fixture.componentInstance;
  }

  afterEach(() => httpMock.verify());

  it('selektor klienta zasilany jest przez GET /customers i auto-wybiera pierwszego klienta', () => {
    const page = createPage();
    expect((page as any).customers()).toEqual(CUSTOMERS);
    expect((page as any).selectedCustomerId()).toBe('CUST-001');
  });

  it('onCustomerChange() przełącza klienta i przeładowuje dyspozycje/historię/profil', () => {
    const page = createPage();

    page.onCustomerChange('CUST-002');

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

    expect((page as any).selectedCustomerId()).toBe('CUST-002');
  });

  it('linkCase() wywołuje PUT /contact-center/dispositions/{id}/case z wpisanym caseId', async () => {
    const page = createPage();
    const target = DISPOSITIONS[0];
    page.onCaseIdDraftChange(target, 'CS-99999');

    const linkPromise = page.linkCase(target);
    const req = httpMock.expectOne(
      `${apiBaseUrl.bankingBackend}/contact-center/dispositions/${target.id}/case`,
    );
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ caseId: 'CS-99999' });
    req.flush({ ...target, linkedCase: 'CS-99999' });
    await linkPromise;

    expect((page as any).dispositions().find((d: any) => d.id === target.id)?.linkedCase).toBe(
      'CS-99999',
    );
  });

  it('linkCase() nie wywołuje backendu gdy pole caseId jest puste', async () => {
    const page = createPage();
    const target = DISPOSITIONS[0];

    await page.linkCase(target);

    httpMock.expectNone(
      `${apiBaseUrl.bankingBackend}/contact-center/dispositions/${target.id}/case`,
    );
  });

  it('linkCase() obsługuje błąd 400 z backendu bez przerywania przepływu', async () => {
    const page = createPage();
    const target = DISPOSITIONS[0];
    page.onCaseIdDraftChange(target, 'CS-10231');

    const linkPromise = page.linkCase(target);
    const req = httpMock.expectOne(
      `${apiBaseUrl.bankingBackend}/contact-center/dispositions/${target.id}/case`,
    );
    req.flush({ message: 'different customer' }, { status: 400, statusText: 'Bad Request' });

    await linkPromise;
  });
});
