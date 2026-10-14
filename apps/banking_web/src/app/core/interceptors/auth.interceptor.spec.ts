import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApiBaseUrlService } from '../services/api-base-url.service';
import { AuthService } from '../services/auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;
  let auth: AuthService;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  async function loginWithToken(token: string): Promise<void> {
    const promise = auth.login({ employeeId: 'PRC-00001', password: 'Admin12345' });
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/auth/login`)
      .flush({ token, employeeId: 'PRC-00001', role: 'Administrator systemu', permissions: [] });
    await promise;
  }

  it('adds the Authorization header for banking_backend requests once authenticated', async () => {
    await loginWithToken('jwt-token-value');

    httpClient.get(`${apiBaseUrl.bankingBackend}/loans`).subscribe();
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans`);
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-token-value');
    req.flush([]);
  });

  it('does not add the Authorization header when unauthenticated', () => {
    httpClient.get(`${apiBaseUrl.bankingBackend}/loans`).subscribe();
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/loans`);
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  });

  it('does not add the Authorization header to /auth/login even with a stale stored token', async () => {
    await loginWithToken('stale-token');

    httpClient
      .post(`${apiBaseUrl.bankingBackend}/auth/login`, { employeeId: 'x', password: 'y' })
      .subscribe();
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/auth/login`);
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({ token: 'new', employeeId: 'x', role: 'Administrator systemu', permissions: [] });
  });

  it('does not add the Authorization header to requests outside banking_backend', async () => {
    await loginWithToken('jwt-token-value');

    httpClient.get('http://localhost:10525/auth/accounts').subscribe();
    const req = httpMock.expectOne('http://localhost:10525/auth/accounts');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  });
});
