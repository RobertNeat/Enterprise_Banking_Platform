import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { LoginPage } from './login.page';

describe('LoginPage', () => {
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;
  let router: Router;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  function createPage() {
    const fixture = TestBed.createComponent(LoginPage);
    fixture.detectChanges();
    return fixture;
  }

  it('does nothing when the form is invalid', async () => {
    const fixture = createPage();
    const component = fixture.componentInstance as unknown as {
      onSubmit(): Promise<void>;
    };

    await component.onSubmit();

    httpMock.expectNone(`${apiBaseUrl.bankingBackend}/auth/login`);
  });

  it('logs in and navigates to /dashboard on success', async () => {
    const fixture = createPage();
    const component = fixture.componentInstance as any;
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    component.form.setValue({ employeeId: 'PRC-00001', password: 'Admin12345' });

    const submitPromise = component.onSubmit();
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/auth/login`).flush({
      token: 'jwt-token-value',
      employeeId: 'PRC-00001',
      role: 'Administrator systemu',
      permissions: ['dashboard'],
    });
    await submitPromise;

    expect(navigateSpy).toHaveBeenCalledWith(['/dashboard']);
    expect(component.errorKey()).toBeNull();
  });

  it('shows an error message and does not navigate on 401', async () => {
    const fixture = createPage();
    const component = fixture.componentInstance as any;
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    component.form.setValue({ employeeId: 'PRC-00001', password: 'wrongpass' });

    const submitPromise = component.onSubmit();
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/auth/login`)
      .flush({ message: 'bad credentials' }, { status: 401, statusText: 'Unauthorized' });
    await submitPromise;

    expect(navigateSpy).not.toHaveBeenCalled();
    expect(component.errorKey()).toBe('auth.login.error.invalidCredentials');
  });
});
