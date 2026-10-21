import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { MessageService } from 'primeng/api';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { TranslationService } from '../../core/services/translation.service';
import { LOAN_APPLICATIONS } from '../loans/loans.data';
import {
  ICT_INCIDENTS,
  KPI_SNAPSHOT,
  PORTFOLIO_TREND,
  RECENT_CASES,
  REGULATORY_ALERTS,
} from './dashboard.data';
import { DashboardPage } from './dashboard.page';

describe('DashboardPage', () => {
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [DashboardPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), MessageService],
    });
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
    // Deterministic formatting regardless of the test runner's detected locale.
    TestBed.inject(TranslationService).setLang('en');
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.removeItem('bank.lang');
  });

  function flushAll(): void {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/dashboard/recent-cases`).flush(RECENT_CASES);
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/dashboard/regulatory-alerts`)
      .flush(REGULATORY_ALERTS);
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/dashboard/ict-incidents`).flush(ICT_INCIDENTS);
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/dashboard/kpi-snapshot/latest`)
      .flush(KPI_SNAPSHOT);
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/dashboard/portfolio-trend`)
      .flush(PORTFOLIO_TREND);
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans`).flush(LOAN_APPLICATIONS);
  }

  it('fetches recent cases, regulatory alerts and ICT incidents from banking_backend', () => {
    const fixture = TestBed.createComponent(DashboardPage);
    const component = fixture.componentInstance;
    flushAll();

    expect((component as any).recentCases()).toEqual(RECENT_CASES);
    expect((component as any).regulatoryAlerts()).toEqual(REGULATORY_ALERTS);
    expect((component as any).ictIncidents()).toEqual(ICT_INCIDENTS);
  });

  it('fills KPI tiles from GET /dashboard/kpi-snapshot/latest, formatted per tile', () => {
    const fixture = TestBed.createComponent(DashboardPage);
    const component = fixture.componentInstance;

    // Placeholder before the snapshot resolves.
    expect((component as any).kpiLoansActive()).toBe('—');
    expect((component as any).kpiLoansValue()).toBe('—');

    flushAll();

    expect((component as any).kpiLoansActive()).toBe('18,420');
    expect((component as any).kpiLoansValue()).toBe('512.4 M PLN');
    expect((component as any).kpiCardsIssued()).toBe('64,910');
    expect((component as any).kpiNpl()).toBe('3.8%');
    expect((component as any).kpiLcr()).toBe('142%');
    expect((component as any).kpiNsfr()).toBe('118%');
    expect((component as any).kpiCasesOpen()).toBe('312');
    expect((component as any).kpiCollectionsOverdue()).toBe('24.6 M PLN');
  });

  it('falls back to the placeholder when the KPI snapshot is not seeded (404)', () => {
    const fixture = TestBed.createComponent(DashboardPage);
    const component = fixture.componentInstance;

    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/dashboard/recent-cases`).flush(RECENT_CASES);
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/dashboard/regulatory-alerts`)
      .flush(REGULATORY_ALERTS);
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/dashboard/ict-incidents`).flush(ICT_INCIDENTS);
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/dashboard/kpi-snapshot/latest`)
      .flush('Not Found', { status: 404, statusText: 'Not Found' });
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/dashboard/portfolio-trend`)
      .flush(PORTFOLIO_TREND);
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans`).flush(LOAN_APPLICATIONS);

    expect((component as any).kpiLoansActive()).toBe('—');
  });

  it('builds the portfolio trend chart from GET /dashboard/portfolio-trend, scaled to millions', () => {
    const fixture = TestBed.createComponent(DashboardPage);
    const component = fixture.componentInstance;
    flushAll();

    const trendData = (component as any).trendData();
    expect(trendData.labels.length).toBe(PORTFOLIO_TREND.length);
    expect(trendData.datasets[0].data).toEqual(
      PORTFOLIO_TREND.map((point) => point.portfolioTotal / 1_000_000),
    );
  });

  it('aggregates GET /loans by product for the product chart (counts, not the KPI scale)', () => {
    const fixture = TestBed.createComponent(DashboardPage);
    const component = fixture.componentInstance;
    flushAll();

    const productData = (component as any).productData();
    // LOAN_PRODUCTS order: cash, mortgage, overdraft, investment, creditLine.
    // LOAN_APPLICATIONS counts: cash x2, mortgage x2, overdraft x1, investment x2, creditLine x1.
    expect(productData.datasets[0].data).toEqual([2, 2, 1, 2, 1]);
  });

  it('resolveIctIncident posts to the resolve endpoint and refetches the ICT incidents list', async () => {
    const fixture = TestBed.createComponent(DashboardPage);
    const component = fixture.componentInstance;
    flushAll();

    const resolvePromise = (component as any).resolveIctIncident(ICT_INCIDENTS[0]);

    httpMock
      .expectOne(
        `${apiBaseUrl.bankingBackend}/dashboard/ict-incidents/${ICT_INCIDENTS[0].id}/resolve`,
      )
      .flush({ ...ICT_INCIDENTS[0], status: 'RESOLVED' });
    // The refetch is kicked off from inside firstValueFrom's success continuation, which runs as
    // a microtask after this synchronous block — yield once before asserting the second request.
    await Promise.resolve();
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/dashboard/ict-incidents`)
      .flush([ICT_INCIDENTS[1]]);
    await resolvePromise;

    expect((component as any).ictIncidents()).toEqual([ICT_INCIDENTS[1]]);
  });

  it('resolveIctIncident does not refetch when the backend call fails', async () => {
    const fixture = TestBed.createComponent(DashboardPage);
    const component = fixture.componentInstance;
    flushAll();

    const resolvePromise = (component as any).resolveIctIncident(ICT_INCIDENTS[0]);

    httpMock
      .expectOne(
        `${apiBaseUrl.bankingBackend}/dashboard/ict-incidents/${ICT_INCIDENTS[0].id}/resolve`,
      )
      .flush('Conflict', { status: 409, statusText: 'Conflict' });
    await resolvePromise;

    expect((component as any).ictIncidents()).toEqual(ICT_INCIDENTS);
  });
});
