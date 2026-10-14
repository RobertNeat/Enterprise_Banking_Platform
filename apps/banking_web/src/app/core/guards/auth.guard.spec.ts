import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

describe('authGuard', () => {
  let router: Router;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    router = TestBed.inject(Router);
  });

  afterEach(() => sessionStorage.clear());

  function runGuard(): boolean | UrlTree {
    return TestBed.runInInjectionContext(() => authGuard({} as never, {} as never)) as
      boolean | UrlTree;
  }

  it('redirects to /login when there is no session', () => {
    const result = runGuard();

    expect(result).not.toBe(true);
    expect((result as UrlTree).toString()).toBe(router.createUrlTree(['/login']).toString());
  });

  it('allows navigation when a session is present', () => {
    sessionStorage.setItem(
      'bank.auth.session',
      JSON.stringify({
        token: 'jwt-token-value',
        employeeId: 'PRC-00001',
        role: 'Administrator systemu',
        permissions: ['dashboard'],
      }),
    );
    TestBed.inject(AuthService);

    const result = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));

    expect(result).toBe(true);
  });
});
