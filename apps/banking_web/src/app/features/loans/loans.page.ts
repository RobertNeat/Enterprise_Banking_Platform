import { Component, computed, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { TranslationService } from '../../core/services/translation.service';
import { StatusTagComponent } from '../../shared/components/status-tag.component';
import { LOAN_APPLICATIONS } from './loans.data';
import { LOAN_STAGE_SEVERITY, LoanSegment } from './loans.model';
import { inject } from '@angular/core';

@Component({
  selector: 'app-loans-page',
  standalone: true,
  imports: [
    DecimalPipe,
    FormsModule,
    TableModule,
    ButtonModule,
    SelectModule,
    IconFieldModule,
    InputIconModule,
    InputTextModule,
    TranslatePipe,
    StatusTagComponent,
  ],
  templateUrl: './loans.page.html',
})
export class LoansPage {
  private readonly router = inject(Router);
  protected readonly i18n = inject(TranslationService);

  protected readonly applications = LOAN_APPLICATIONS;
  protected readonly stageSeverity = LOAN_STAGE_SEVERITY;

  protected readonly searchTerm = signal('');
  protected readonly segmentFilter = signal<LoanSegment | 'all'>('all');

  protected readonly segmentOptions = computed(() => [
    { label: this.i18n.translate('common.all'), value: 'all' as const },
    { label: this.i18n.translate('loans.segment.retail'), value: 'retail' as const },
    { label: this.i18n.translate('loans.segment.business'), value: 'business' as const },
  ]);

  protected readonly filtered = computed(() => {
    const term = this.searchTerm().toLowerCase();
    const segment = this.segmentFilter();
    return this.applications.filter((a) => {
      const matchesSegment = segment === 'all' || a.segment === segment;
      const matchesTerm =
        !term || a.applicant.toLowerCase().includes(term) || a.id.toLowerCase().includes(term);
      return matchesSegment && matchesTerm;
    });
  });

  openDetail(id: string): void {
    this.router.navigate(['/loans', id]);
  }
}
