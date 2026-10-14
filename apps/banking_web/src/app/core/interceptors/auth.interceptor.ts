import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { AuthService } from '../services/auth.service';
import { ApiBaseUrlService } from '../services/api-base-url.service';

/** Routes that must stay anonymous even if a (possibly stale/expired) token is still stored. */
const UNAUTHENTICATED_PATHS = ['/auth/login', '/auth/register'];

/** Attaches the bearer token only to requests aimed at banking_backend, never other origins. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const apiBaseUrl = inject(ApiBaseUrlService);

  const isBackendRequest = req.url.startsWith(apiBaseUrl.bankingBackend);
  const isUnauthenticatedPath = UNAUTHENTICATED_PATHS.some((path) => req.url.endsWith(path));
  const token = auth.token();

  if (token && isBackendRequest && !isUnauthenticatedPath) {
    return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
  }
  return next(req);
};
