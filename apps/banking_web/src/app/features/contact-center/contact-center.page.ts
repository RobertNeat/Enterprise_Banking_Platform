import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { TranslationService } from '../../core/services/translation.service';
import { NotificationService } from '../../core/services/notification.service';
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { CASE_SEVERITY } from '../cases/cases.model';
import {
  CONTACT_HISTORY,
  CUSTOMER_PROFILE,
  DISPOSITIONS,
  INITIAL_CHAT,
} from './contact-center.data';
import { ChatMessage } from './contact-center.model';

@Component({
  selector: 'app-contact-center-page',
  standalone: true,
  imports: [
    FormsModule,
    ButtonModule,
    InputTextModule,
    TableModule,
    TranslatePipe,
    StatusTagComponent,
  ],
  templateUrl: './contact-center.page.html',
})
export class ContactCenterPage {
  private readonly notify = inject(NotificationService);
  private readonly i18n = inject(TranslationService);

  protected readonly profile = CUSTOMER_PROFILE;
  protected readonly dispositions = DISPOSITIONS;
  protected readonly history = CONTACT_HISTORY;
  protected readonly caseSeverity = CASE_SEVERITY;

  protected readonly messages = signal<ChatMessage[]>(INITIAL_CHAT);
  protected readonly draft = signal('');

  sendMessage(): void {
    const text = this.draft().trim();
    if (!text) return;
    this.messages.update((msgs) => [
      ...msgs,
      {
        from: 'customer',
        text,
        time: new Date().toLocaleTimeString('pl-PL', { hour: '2-digit', minute: '2-digit' }),
      },
    ]);
    this.draft.set('');
    this.notify.info('contact.chatMock');
  }
}
