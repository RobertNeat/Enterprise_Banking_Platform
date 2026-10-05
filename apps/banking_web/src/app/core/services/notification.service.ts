import { Injectable, inject } from '@angular/core';
import { MessageService } from 'primeng/api';
import { TranslationService } from './translation.service';

export type NotifySeverity = 'info' | 'success' | 'warn' | 'error';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly messageService = inject(MessageService);
  private readonly i18n = inject(TranslationService);

  info(detailKey: string, params?: Record<string, string | number>): void {
    this.show('info', detailKey, params);
  }

  success(detailKey: string, params?: Record<string, string | number>): void {
    this.show('success', detailKey, params);
  }

  warn(detailKey: string, params?: Record<string, string | number>): void {
    this.show('warn', detailKey, params);
  }

  error(detailKey: string, params?: Record<string, string | number>): void {
    this.show('error', detailKey, params);
  }

  private show(
    severity: NotifySeverity,
    detailKey: string,
    params?: Record<string, string | number>,
  ): void {
    this.messageService.add({
      severity,
      summary: this.i18n.translate(`toast.${severity}.title`),
      detail: this.i18n.translate(detailKey, params),
      life: severity === 'error' ? 7000 : 4500,
    });
  }
}
