import { Component, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { TranslationService } from '../../core/services/translation.service';
import { NotificationService } from '../../core/services/notification.service';
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { NEUTRAL_TAG_SEVERITY } from '../../shared/utils/status-severity';
import { EbankingStore } from './ebanking.store';
import { ACCESS_SEVERITY, EbankingDevice, EbankingUser } from './ebanking.model';

type OtpPurpose = 'unlock' | 'resetDevice';

@Component({
  selector: 'app-ebanking-page',
  standalone: true,
  imports: [
    DecimalPipe,
    ButtonModule,
    DialogModule,
    SelectModule,
    TableModule,
    InputTextModule,
    FormsModule,
    TranslatePipe,
    StatusTagComponent,
  ],
  templateUrl: './ebanking.page.html',
})
export class EbankingPage {
  private readonly notify = inject(NotificationService);
  private readonly store = inject(EbankingStore);
  protected readonly i18n = inject(TranslationService);

  protected readonly users = this.store.users;
  protected readonly accessSeverity = ACCESS_SEVERITY;
  protected readonly neutralSeverity = NEUTRAL_TAG_SEVERITY;

  // --- Dwuetapowy flow OTP (unlock i reset-device współdzielą ten sam dialog/stan) ---

  protected readonly otpDialogOpen = signal(false);
  protected readonly otpPurpose = signal<OtpPurpose | null>(null);
  protected readonly otpStep = signal<1 | 2>(1);
  protected readonly otpUser = signal<EbankingUser | null>(null);
  protected readonly otpDevices = signal<EbankingDevice[]>([]);
  protected readonly otpSelectedDeviceId = signal<string | null>(null);
  protected readonly otpId = signal<string | null>(null);
  protected readonly otpExpiresAt = signal<string | null>(null);
  protected readonly otpCode = signal('');
  protected readonly otpSending = signal(false);
  protected readonly otpConfirming = signal(false);
  protected readonly otpError = signal(false);

  protected readonly otpTitleKey = computed(() =>
    this.otpPurpose() === 'resetDevice'
      ? 'ebanking.otp.resetDeviceTitle'
      : 'ebanking.otp.unlockTitle',
  );

  protected readonly deviceOptions = computed(() =>
    this.otpDevices().map((d) => ({ label: `${d.model} (${d.deviceId})`, value: d.deviceId })),
  );

  unlock(user: EbankingUser): void {
    if (user.accessStatus === 'active') {
      this.notify.warn('toast.actionBlocked');
      return;
    }
    this.resetOtpState();
    this.otpPurpose.set('unlock');
    this.otpUser.set(user);
    this.otpDialogOpen.set(true);
  }

  async resetDevice(user: EbankingUser): Promise<void> {
    this.resetOtpState();
    this.otpPurpose.set('resetDevice');
    this.otpUser.set(user);
    try {
      const devices = await this.store.listDevices(user.id);
      this.otpDevices.set(devices);
      if (devices.length === 1) {
        this.otpSelectedDeviceId.set(devices[0].deviceId);
      }
      this.otpDialogOpen.set(true);
    } catch {
      this.notify.error('ebanking.error.generic');
    }
  }

  closeOtpDialog(): void {
    this.otpDialogOpen.set(false);
    this.resetOtpState();
  }

  private resetOtpState(): void {
    this.otpStep.set(1);
    this.otpPurpose.set(null);
    this.otpUser.set(null);
    this.otpDevices.set([]);
    this.otpSelectedDeviceId.set(null);
    this.otpId.set(null);
    this.otpExpiresAt.set(null);
    this.otpCode.set('');
    this.otpError.set(false);
  }

  async sendOtp(): Promise<void> {
    const user = this.otpUser();
    if (!user) {
      return;
    }
    this.otpSending.set(true);
    try {
      const challenge =
        this.otpPurpose() === 'resetDevice'
          ? await this.store.requestDeviceResetOtp(user.id, this.otpSelectedDeviceId()!)
          : await this.store.requestUnlockOtp(user.id);
      this.otpId.set(challenge.otpId);
      this.otpExpiresAt.set(challenge.expiresAt);
      this.otpStep.set(2);
    } catch {
      this.notify.error('ebanking.error.generic');
    } finally {
      this.otpSending.set(false);
    }
  }

  async confirmOtp(): Promise<void> {
    const user = this.otpUser();
    const otpId = this.otpId();
    if (!user || !otpId || !this.otpCode()) {
      return;
    }
    this.otpConfirming.set(true);
    this.otpError.set(false);
    try {
      if (this.otpPurpose() === 'resetDevice') {
        await this.store.confirmDeviceReset(
          user.id,
          this.otpSelectedDeviceId()!,
          otpId,
          this.otpCode(),
        );
      } else {
        await this.store.confirmUnlock(user.id, otpId, this.otpCode());
      }
      this.notify.success('toast.saved');
      this.closeOtpDialog();
    } catch {
      this.otpError.set(true);
    } finally {
      this.otpConfirming.set(false);
    }
  }
}
