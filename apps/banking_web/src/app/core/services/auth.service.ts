import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { ApiBaseUrlService } from './api-base-url.service';

export interface LoginRequest {
  employeeId: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  employeeId: string;
  role: string;
  permissions: string[];
  seniorityLevel: number;
}

interface StoredSession {
  token: string;
  employeeId: string;
  role: string;
  permissions: string[];
  /**
   * Optional for backward compatibility with a session stored before this field existed —
   * an old sessionStorage entry from a previous build must not crash login state on reload.
   */
  seniorityLevel?: number;
}

/** sessionStorage, not localStorage — token must not survive beyond the browser tab's session. */
const STORAGE_KEY = 'bank.auth.session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(ApiBaseUrlService);

  private readonly sessionSignal = signal<StoredSession | null>(this.loadStoredSession());

  readonly token = computed(() => this.sessionSignal()?.token ?? null);
  readonly role = computed(() => this.sessionSignal()?.role ?? null);
  readonly employeeId = computed(() => this.sessionSignal()?.employeeId ?? null);
  readonly permissions = computed(() => this.sessionSignal()?.permissions ?? []);
  /** 0 (lowest, "nobody") for a pre-R1.5 stored session missing this field. */
  readonly seniorityLevel = computed(() => this.sessionSignal()?.seniorityLevel ?? 0);
  readonly isAuthenticated = computed(() => this.sessionSignal() !== null);

  async login(request: LoginRequest): Promise<LoginResponse> {
    const response = await firstValueFrom(
      this.http.post<LoginResponse>(`${this.apiBaseUrl.bankingBackend}/auth/login`, request),
    );
    const session: StoredSession = {
      token: response.token,
      employeeId: response.employeeId,
      role: response.role,
      permissions: response.permissions,
      seniorityLevel: response.seniorityLevel,
    };
    this.sessionSignal.set(session);
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    return response;
  }

  async logout(): Promise<void> {
    try {
      await firstValueFrom(
        this.http.post<void>(`${this.apiBaseUrl.bankingBackend}/auth/logout`, {}),
      );
    } finally {
      this.sessionSignal.set(null);
      sessionStorage.removeItem(STORAGE_KEY);
    }
  }

  private loadStoredSession(): StoredSession | null {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as StoredSession;
    } catch {
      return null;
    }
  }
}
