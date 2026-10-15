import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { EbankingDevice, EbankingUser, OtpChallenge } from './ebanking.model';

/**
 * Zasilane z banking_backend (GET /ebanking/users). F5 — odblokowanie dostępu i reset urządzenia
 * idą przez dwuetapowy flow OTP/SCA (identity_access_mock), proxy w banking_backend — zobacz
 * docs/services/banking_backend.md.
 */
@Injectable({ providedIn: 'root' })
export class EbankingStore {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(ApiBaseUrlService);

  private readonly items = signal<EbankingUser[]>([]);

  readonly users = this.items.asReadonly();

  constructor() {
    this.http
      .get<EbankingUser[]>(`${this.apiBaseUrl.bankingBackend}/ebanking/users`)
      .subscribe((users) => this.items.set(users));
  }

  /** F5 step 1 — issues an UNLOCK_ACCESS OTP challenge. */
  async requestUnlockOtp(userId: string): Promise<OtpChallenge> {
    return firstValueFrom(
      this.http.post<OtpChallenge>(
        `${this.apiBaseUrl.bankingBackend}/ebanking/${userId}/unlock/request-otp`,
        {},
      ),
    );
  }

  /** F5 step 2 — verifies the OTP and unlocks the access; backend rejects (409) if already active. */
  async confirmUnlock(userId: string, otpId: string, code: string): Promise<EbankingUser> {
    const updated = await firstValueFrom(
      this.http.post<EbankingUser>(
        `${this.apiBaseUrl.bankingBackend}/ebanking/${userId}/unlock/confirm`,
        { otpId, code },
      ),
    );
    this.replace(updated);
    return updated;
  }

  /** Lists the user's registered devices (proxy to identity_access_mock) for the reset-device picker. */
  async listDevices(userId: string): Promise<EbankingDevice[]> {
    return firstValueFrom(
      this.http.get<EbankingDevice[]>(
        `${this.apiBaseUrl.bankingBackend}/ebanking/${userId}/devices`,
      ),
    );
  }

  /** F5 step 1 (device reset) — issues a RESET_DEVICE OTP challenge. */
  async requestDeviceResetOtp(userId: string, deviceId: string): Promise<OtpChallenge> {
    return firstValueFrom(
      this.http.post<OtpChallenge>(
        `${this.apiBaseUrl.bankingBackend}/ebanking/${userId}/devices/${deviceId}/reset/request-otp`,
        {},
      ),
    );
  }

  /** F5 step 2 (device reset) — verifies the OTP and resets/unbinds the device. */
  async confirmDeviceReset(
    userId: string,
    deviceId: string,
    otpId: string,
    code: string,
  ): Promise<void> {
    await firstValueFrom(
      this.http.post<void>(
        `${this.apiBaseUrl.bankingBackend}/ebanking/${userId}/devices/${deviceId}/reset/confirm`,
        { otpId, code },
      ),
    );
  }

  private replace(user: EbankingUser): void {
    this.items.update((list) => list.map((item) => (item.id === user.id ? user : item)));
  }
}
