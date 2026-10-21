import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { RegisterPage } from './register.page';

describe('RegisterPage', () => {
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RegisterPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        MessageService,
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    httpMock.verify();
  });

  function createPage() {
    const fixture = TestBed.createComponent(RegisterPage);
    fixture.detectChanges();
    return fixture;
  }

  function fillValidForm(component: RegisterPage) {
    component['form'].setValue({
      personal: {
        firstName: 'Jan',
        lastName: 'Kowalski',
        email: 'jan.kowalski@bank.test',
        phone: '123456789',
      },
      employment: {
        employeeId: 'PRC-77001',
        department: 'IT',
        branch: 'Warszawa',
        position: 'Analityk',
        supervisor: 'Anna Nowak',
        startDate: new Date(2026, 9, 14),
      },
      industry: {
        businessLines: ['retail'],
        certifications: '',
        licenseNumber: '',
      },
      account: {
        password: 'Secret1234',
        confirmPassword: 'Secret1234',
        acceptPolicy: true,
      },
    });
  }

  it('does nothing when the form is invalid', async () => {
    const fixture = createPage();
    const component = fixture.componentInstance as unknown as { onSubmit(): Promise<void> };

    await component.onSubmit();

    httpMock.expectNone(`${apiBaseUrl.bankingBackend}/auth/register`);
  });

  it('registers and navigates to /login on success', async () => {
    const fixture = createPage();
    const component = fixture.componentInstance;
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fillValidForm(component);

    const submitPromise = component.onSubmit();
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/auth/register`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      personal: {
        firstName: 'Jan',
        lastName: 'Kowalski',
        email: 'jan.kowalski@bank.test',
        phone: '123456789',
      },
      employment: {
        employeeId: 'PRC-77001',
        department: 'IT',
        branch: 'Warszawa',
        position: 'Analityk',
        supervisor: 'Anna Nowak',
        startDate: '2026-10-14T00:00:00.000Z',
      },
      industry: { businessLines: ['retail'], certifications: '', licenseNumber: '' },
      account: { password: 'Secret1234', confirmPassword: 'Secret1234', acceptPolicy: true },
    });
    req.flush({
      id: 'uuid-1',
      employeeId: 'PRC-77001',
      role: null,
      status: 'PENDING_APPROVAL',
      firstName: 'Jan',
      lastName: 'Kowalski',
      email: 'jan.kowalski@bank.test',
    });
    await submitPromise;

    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
    expect(component['errorKey']()).toBeNull();
    expect(component['submitting']()).toBe(false);
  });

  it('shows an error message and does not navigate on 409 (employeeId already registered)', async () => {
    const fixture = createPage();
    const component = fixture.componentInstance;
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fillValidForm(component);

    const submitPromise = component.onSubmit();
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/auth/register`)
      .flush({ message: 'employeeId already exists' }, { status: 409, statusText: 'Conflict' });
    await submitPromise;

    expect(navigateSpy).not.toHaveBeenCalled();
    expect(component['errorKey']()).toBe('auth.register.error.employeeIdTaken');
    expect(component['submitting']()).toBe(false);
  });
});
