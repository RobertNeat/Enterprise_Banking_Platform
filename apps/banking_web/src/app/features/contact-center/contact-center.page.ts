import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { TranslationService } from '../../core/services/translation.service';
import { NotificationService } from '../../core/services/notification.service';
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { CASE_SEVERITY } from '../cases/cases.model';
import { INITIAL_CHAT } from './contact-center.data';
import { ChatMessage, CustomerProfile, ServiceDisposition } from './contact-center.model';
import { ContactCenterStore } from './contact-center.store';

const EMPTY_PROFILE: CustomerProfile = {
  name: '',
  segment: '',
  loansCount: 0,
  cardsCount: 0,
  riskRating: '',
  openCases: 0,
};

@Component({
  selector: 'app-contact-center-page',
  standalone: true,
  imports: [
    FormsModule,
    ButtonModule,
    InputTextModule,
    SelectModule,
    TableModule,
    TranslatePipe,
    StatusTagComponent,
  ],
  templateUrl: './contact-center.page.html',
})
export class ContactCenterPage {
  private readonly notify = inject(NotificationService);
  private readonly i18n = inject(TranslationService);
  private readonly store = inject(ContactCenterStore);

  protected readonly profile = computed(() => this.store.profile() ?? EMPTY_PROFILE);
  protected readonly dispositions = this.store.dispositions;
  protected readonly history = this.store.history;
  protected readonly caseSeverity = CASE_SEVERITY;

  protected readonly customers = this.store.customers;
  protected readonly selectedCustomerId = this.store.selectedCustomerId;
  protected readonly customerOptions = computed(() =>
    this.customers().map((c) => ({ label: `${c.fullName} (${c.id})`, value: c.id })),
  );

  protected readonly messages = signal<ChatMessage[]>(INITIAL_CHAT);
  protected readonly draft = signal('');

  /** KKT — zmiana wybranego klienta w selektorze (zasilanym GET /customers). */
  onCustomerChange(customerId: string): void {
    if (!customerId) return;
    this.store.selectCustomer(customerId);
  }

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

  // --- KKT/F8 — powiązanie dyspozycji ze sprawą ---

  protected readonly caseIdDrafts = signal<Record<string, string>>({});

  caseIdDraft(disposition: ServiceDisposition): string {
    return this.caseIdDrafts()[disposition.id] ?? '';
  }

  onCaseIdDraftChange(disposition: ServiceDisposition, value: string): void {
    this.caseIdDrafts.update((drafts) => ({ ...drafts, [disposition.id]: value }));
  }

  async linkCase(disposition: ServiceDisposition): Promise<void> {
    const caseId = this.caseIdDraft(disposition).trim();
    if (!caseId) return;
    try {
      await this.store.linkCase(disposition.id, caseId);
      this.notify.success('toast.saved');
      this.onCaseIdDraftChange(disposition, '');
    } catch {
      this.notify.error('contact.linkCase.error');
    }
  }
}
