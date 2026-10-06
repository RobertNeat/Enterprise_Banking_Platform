import { Component, computed, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { StepperModule } from 'primeng/stepper';
import { TableModule } from 'primeng/table';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { NotificationService } from '../../core/services/notification.service';
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { LOAN_APPLICATIONS, buildSchedule } from './loans.data';
import { INSTALLMENT_SEVERITY, LOAN_STAGES, LOAN_STAGE_SEVERITY } from './loans.model';
import { LoansStore } from './loans.store';

@Component({
  selector: 'app-loan-detail-page',
  standalone: true,
  imports: [
    DecimalPipe,
    ButtonModule,
    StepperModule,
    TableModule,
    TranslatePipe,
    StatusTagComponent,
  ],
  templateUrl: './loan-detail.page.html',
})
export class LoanDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly notify = inject(NotificationService);
  private readonly store = inject(LoansStore);

  protected readonly stages = LOAN_STAGES;
  protected readonly stageSeverity = LOAN_STAGE_SEVERITY;
  protected readonly installmentSeverity = INSTALLMENT_SEVERITY;

  protected readonly loan = computed(() => {
    const id = this.route.snapshot.paramMap.get('id');
    return this.store.findById(id ?? '') ?? LOAN_APPLICATIONS[0];
  });

  protected readonly stageIndex = computed(() => this.stages.indexOf(this.loan().stage));

  protected readonly schedule = computed(() => {
    const amount = this.loan().approvedAmount ?? this.loan().requestedAmount;
    return buildSchedule(amount, 12);
  });

  back(): void {
    this.router.navigate(['/loans']);
  }

  advanceStage(): void {
    const idx = this.stageIndex();
    if (idx >= this.stages.length - 1) {
      this.notify.warn('toast.actionBlocked');
      return;
    }
    this.notify.success('toast.saved');
  }
}
