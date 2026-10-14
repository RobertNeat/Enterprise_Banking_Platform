import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { LOAN_APPLICATIONS } from './loans.data';
import { LoanDetailPage } from './loan-detail.page';

describe('LoanDetailPage', () => {
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;

  function configure(loanId: string) {
    TestBed.configureTestingModule({
      imports: [LoanDetailPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        MessageService,
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: loanId }) } },
        },
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
  }

  afterEach(() => httpMock.verify());

  it('always fetches /loans/{id} so it gets the real installments, not just on a deep link', () => {
    configure('LN-80231');

    const fixture = TestBed.createComponent(LoanDetailPage);
    const component = fixture.componentInstance;

    // LoansStore itself also loads its own list in the background.
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans`).flush(LOAN_APPLICATIONS);

    const seededInstallments = [
      {
        no: 1,
        dueDate: '2026-10-12',
        principal: 1916.67,
        interest: 3028.33,
        status: 'upcoming' as const,
      },
    ];
    const detailReq = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans/LN-80231`);
    expect(detailReq.request.method).toBe('GET');
    detailReq.flush({
      ...LOAN_APPLICATIONS[0],
      installments: seededInstallments,
      stageTransitions: [],
    });

    expect((component as any).loan().id).toBe('LN-80231');
    expect((component as any).schedule()).toEqual(seededInstallments);
  });

  it('falls back to EMPTY_LOAN / locally-generated schedule when the backend has no such loan', () => {
    configure('LN-99999');

    const fixture = TestBed.createComponent(LoanDetailPage);
    const component = fixture.componentInstance;

    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans`).flush([]);
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/loans/LN-99999`)
      .flush({ message: 'not found' }, { status: 404, statusText: 'Not Found' });

    expect((component as any).loan().id).toBe('');
  });
});
