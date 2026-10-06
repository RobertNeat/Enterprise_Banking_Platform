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
  // label() ma priorytet nad labelKey() — służy do wartości, które NIE są
  // kluczami i18n (np. nazwa sieci karty, nazwa uprawnienia z danych).
  template: `<p-tag
    [id]="tagId() ? tagId() + '-tag' : null"
    [value]="label() ?? (labelKey()! | translate)"
    [severity]="severity()"
  />`,
})
export class StatusTagComponent {
  /** Id przekazywany przez wywołującego; trafia na host, p-tag dostaje sufiks '-tag'. */
  readonly tagId = input<string>();
  /** Klucz i18n — ignorowany, jeśli podano label(). */
  readonly labelKey = input<string>();
  /** Surowy tekst (nie-i18n), np. nazwa sieci karty/uprawnienia. */
  readonly label = input<string>();
  readonly severity = input.required<StatusSeverity>();
}
