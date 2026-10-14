import { Injectable } from '@angular/core';

/** banking_backend port — fixed, no environment.* files in this project (see app.config.ts). */
const BANKING_BACKEND_PORT = 10520;

/**
 * Resolves the banking_backend base URL from the browser's own hostname, so the same build
 * works against `localhost`, a LAN IP, or a container hostname without environment files.
 */
@Injectable({ providedIn: 'root' })
export class ApiBaseUrlService {
  readonly bankingBackend = `${window.location.protocol}//${window.location.hostname}:${BANKING_BACKEND_PORT}`;
}
