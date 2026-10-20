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

export interface RegisterRequest {
  personal: {
    firstName: string;
    lastName: string;
    email: string;
    phone: string;
  };
  employment: {
    employeeId: string;
    department: string;
    branch: string;
    position: string;
    supervisor: string;
    /** ISO 8601 string — identity_access_mock stores it as a raw String, no further parsing. */
    startDate: string;
  };
  industry: {
    businessLines: string[];
    certifications: string;
    licenseNumber: string;
  };
  account: {
    password: string;
    confirmPassword: string;
    acceptPolicy: boolean;
  };
}

export interface RegisterResponse {
  id: string;
  employeeId: string;
  role: string | null;
  status: string;
  firstName: string;
  lastName: string;
  email: string;
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

/**
 * Must match RoleCode.ADMIN's seniorityLevel in banking_backend (auth/RoleCode.java) — the
 * highest in the system. No role can ever check an approval requested at this level, so
 * banking_backend blocks submit-for-approval/request-transfer-approval with 403 for it; this is
 * the matching UX-only gate (same pattern as approvals.page.ts's canDecide()).
 */
const MAX_SENIORITY_LEVEL = 3;

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
  /** False for the highest-seniority role (admin) — see MAX_SENIORITY_LEVEL above. */
  readonly canInitiateMakerChecker = computed(() => this.seniorityLevel() < MAX_SENIORITY_LEVEL);

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

  /** Public registration — does not touch the session; account stays PENDING_APPROVAL until an admin approves it. */
  async register(request: RegisterRequest): Promise<RegisterResponse> {
    return firstValueFrom(
      this.http.post<RegisterResponse>(`${this.apiBaseUrl.bankingBackend}/auth/register`, request),
    );
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
