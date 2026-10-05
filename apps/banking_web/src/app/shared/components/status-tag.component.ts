import { Component, input } from '@angular/core';
import { TagModule } from 'primeng/tag';
import { TranslatePipe } from '../../core/services/translate.pipe';

export type StatusSeverity = 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast';

@Component({
  selector: 'app-status-tag',
  standalone: true,
  imports: [TagModule, TranslatePipe],
  template: `<p-tag [value]="labelKey() | translate" [severity]="severity()" />`,
})
export class StatusTagComponent {
  readonly labelKey = input.required<string>();
  readonly severity = input.required<StatusSeverity>();
}
