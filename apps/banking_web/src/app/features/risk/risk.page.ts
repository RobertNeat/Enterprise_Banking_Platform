import { Component, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { ProgressBarModule } from 'primeng/progressbar';
import { TableModule } from 'primeng/table';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { RiskStore } from './risk.store';
import { IFRS9_SEVERITY } from './risk.model';

@Component({
  selector: 'app-risk-page',
  standalone: true,
  imports: [DecimalPipe, TableModule, ProgressBarModule, TranslatePipe, StatusTagComponent],
  templateUrl: './risk.page.html',
})
export class RiskPage {
  private readonly store = inject(RiskStore);

  protected readonly entries = this.store.entries;
  protected readonly stageSeverity = IFRS9_SEVERITY;
}
