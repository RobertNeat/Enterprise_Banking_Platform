import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { ProgressBarModule } from 'primeng/progressbar';
import { TableModule } from 'primeng/table';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { NotificationService } from '../../core/services/notification.service';
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { MoneyPipe } from '../../shared/utils/money.pipe';
import { PercentPipe } from '../../shared/utils/percent.pipe';
import { RiskStore } from './risk.store';
import { CollateralEntry, IFRS9_SEVERITY } from './risk.model';

@Component({
  selector: 'app-risk-page',
  standalone: true,
  imports: [
    MoneyPipe,
    PercentPipe,
    ReactiveFormsModule,
    ButtonModule,
    DialogModule,
    InputNumberModule,
    TableModule,
    ProgressBarModule,
    TranslatePipe,
    StatusTagComponent,
  ],
  templateUrl: './risk.page.html',
})
export class RiskPage {
  private readonly notify = inject(NotificationService);
  private readonly fb = inject(FormBuilder);
  private readonly store = inject(RiskStore);

  protected readonly entries = this.store.entries;
  protected readonly stageSeverity = IFRS9_SEVERITY;

  // --- Dialog: przeliczenie ECL (F3) ---

  protected readonly reclassifyDialogOpen = signal(false);
  protected readonly reclassifyEntry = signal<CollateralEntry | null>(null);
  protected readonly submitting = signal(false);

  protected readonly reclassifyForm = this.fb.nonNullable.group({
    daysPastDue: this.fb.nonNullable.control<number | null>(null, [
      Validators.required,
      Validators.min(0),
    ]),
  });

  private readonly reclassifyFormTick = toSignal(this.reclassifyForm.valueChanges, {
    initialValue: null,
  });

  protected readonly reclassifyFormValid = computed(() => {
    this.reclassifyFormTick();
    return this.reclassifyForm.valid;
  });

  openReclassifyDialog(entry: CollateralEntry): void {
    this.reclassifyEntry.set(entry);
    this.reclassifyForm.reset();
    this.reclassifyDialogOpen.set(true);
  }

  closeReclassifyDialog(): void {
    this.reclassifyDialogOpen.set(false);
    this.reclassifyEntry.set(null);
    this.reclassifyForm.reset();
  }

  async submitReclassify(): Promise<void> {
    const entry = this.reclassifyEntry();
    if (!entry || this.reclassifyForm.invalid) {
      return;
    }
    const { daysPastDue } = this.reclassifyForm.getRawValue();
    this.submitting.set(true);
    try {
      await this.store.reclassify(entry.id, daysPastDue!);
      this.notify.success('toast.saved');
      this.closeReclassifyDialog();
    } catch {
      this.notify.error('risk.error.generic');
    } finally {
      this.submitting.set(false);
    }
  }
}
