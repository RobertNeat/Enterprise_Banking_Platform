import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ApiBaseUrlService } from './api-base-url.service';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;

  const loginResponse = {
    token: 'jwt-token-value',
    employeeId: 'PRC-00001',
    role: 'Administrator systemu',
    permissions: ['dashboard', 'loans'],
    seniorityLevel: 3,
  };

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
    service = TestBed.inject(AuthService);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  it('starts unauthenticated with no stored session', () => {
    expect(service.isAuthenticated()).toBe(false);
    expect(service.token()).toBeNull();
    expect(service.permissions()).toEqual([]);
  });

  it('login posts credentials, stores the session and exposes token/role/permissions', async () => {
    const promise = service.login({ employeeId: 'PRC-00001', password: 'Admin12345' });

    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ employeeId: 'PRC-00001', password: 'Admin12345' });
    req.flush(loginResponse);

    await promise;

    expect(service.isAuthenticated()).toBe(true);
    expect(service.token()).toBe('jwt-token-value');
    expect(service.role()).toBe('Administrator systemu');
    expect(service.permissions()).toEqual(['dashboard', 'loans']);
    expect(service.employeeId()).toBe('PRC-00001');
    expect(service.seniorityLevel()).toBe(3);
    expect(sessionStorage.getItem('bank.auth.session')).toContain('jwt-token-value');
  });

  it('logout posts to /auth/logout and clears the session even if already authenticated', async () => {
    const loginPromise = service.login({ employeeId: 'PRC-00001', password: 'Admin12345' });
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/auth/login`).flush(loginResponse);
    await loginPromise;

    const logoutPromise = service.logout();
    const logoutReq = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/auth/logout`);
    expect(logoutReq.request.method).toBe('POST');
    logoutReq.flush(null, { status: 204, statusText: 'No Content' });

    await logoutPromise;

    expect(service.isAuthenticated()).toBe(false);
    expect(service.token()).toBeNull();
    expect(sessionStorage.getItem('bank.auth.session')).toBeNull();
  });

  it('logout clears the local session even when the backend call fails', async () => {
    const loginPromise = service.login({ employeeId: 'PRC-00001', password: 'Admin12345' });
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/auth/login`).flush(loginResponse);
    await loginPromise;

    const logoutPromise = service.logout();
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/auth/logout`)
      .flush({ message: 'unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    await expect(logoutPromise).rejects.toBeTruthy();
    expect(service.isAuthenticated()).toBe(false);
    expect(sessionStorage.getItem('bank.auth.session')).toBeNull();
  });
});
