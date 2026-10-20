import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { ICT_INCIDENTS, RECENT_CASES, REGULATORY_ALERTS } from './dashboard.data';
import { DashboardPage } from './dashboard.page';

describe('DashboardPage', () => {
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [DashboardPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
  });

  afterEach(() => httpMock.verify());

  it('fetches recent cases, regulatory alerts and ICT incidents from banking_backend', () => {
    const fixture = TestBed.createComponent(DashboardPage);
    const component = fixture.componentInstance;

    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/dashboard/recent-cases`).flush(RECENT_CASES);
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/dashboard/regulatory-alerts`)
      .flush(REGULATORY_ALERTS);
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/dashboard/ict-incidents`).flush(ICT_INCIDENTS);

    expect((component as any).recentCases()).toEqual(RECENT_CASES);
    expect((component as any).regulatoryAlerts()).toEqual(REGULATORY_ALERTS);
    expect((component as any).ictIncidents()).toEqual(ICT_INCIDENTS);
  });
});
