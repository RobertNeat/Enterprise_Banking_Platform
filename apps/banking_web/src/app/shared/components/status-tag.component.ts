import { Component, input } from '@angular/core';
import { TagModule } from 'primeng/tag';
import { TranslatePipe } from '../../core/services/translate.pipe';

export type StatusSeverity = 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast';

@Component({
  selector: 'app-status-tag',
  standalone: true,
  imports: [TagModule, TranslatePipe],
  // Host (app-status-tag) dostaje bazowe tagId; p-tag (realny widoczny
  // element) dostaje sufiks '-tag', żeby uniknąć duplikatu id w DOM.
  host: { '[attr.id]': 'tagId()' },
  template: `<p-tag
    [id]="tagId() ? tagId() + '-tag' : null"
    [value]="labelKey() | translate"
    [severity]="severity()"
  />`,
})
export class StatusTagComponent {
  /** Id przekazywany przez wywołującego; trafia na host, p-tag dostaje sufiks '-tag'. */
  readonly tagId = input<string>();
  readonly labelKey = input.required<string>();
  readonly severity = input.required<StatusSeverity>();
}
