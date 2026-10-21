import { Component, input, output } from '@angular/core';
import { DialogModule } from 'primeng/dialog';
import { TableModule } from 'primeng/table';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { AuditEvent } from './audit-event.model';

/**
 * Współdzielony dialog "Historia" (ślad audytowy z governance_mock) — jedna implementacja dla
 * wniosku kredytowego, sprawy, karty i zajęcia. Strona wywołująca dostarcza [visible]/[events]/
 * [loading] (z AuditHistoryStore) i nasłuchuje (visibleChange) do zamknięcia; samo pobranie danych
 * zostaje po stronie strony (ona zna entityType/entityId), żeby dialog nie musiał tego wiedzieć.
 */
@Component({
  selector: 'app-audit-history-dialog',
  standalone: true,
  imports: [DialogModule, TableModule, TranslatePipe],
  template: `
    <p-dialog
      [id]="dialogId()"
      [pt]="{ pcCloseButton: { root: { id: dialogId() + '-close-btn' } } }"
      [visible]="visible()"
      (visibleChange)="visibleChange.emit($event)"
      [modal]="true"
      [closable]="true"
      [style]="{ width: '40rem', maxWidth: '95vw' }"
      [header]="'history.title' | translate"
    >
      <p-table [id]="dialogId() + '-table'" [value]="events()" [rows]="10" [loading]="loading()">
        <ng-template #header>
          <tr>
            <th>{{ 'history.timestamp' | translate }}</th>
            <th>{{ 'history.action' | translate }}</th>
            <th>{{ 'history.actor' | translate }}</th>
            <th>{{ 'history.details' | translate }}</th>
          </tr>
        </ng-template>
        <ng-template #body let-event>
          <tr [id]="dialogId() + '-row-' + event.id">
            <td>{{ event.timestamp }}</td>
            <td>{{ event.action }}</td>
            <td>{{ event.actor }}</td>
            <td>{{ event.details || '—' }}</td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="4" [id]="dialogId() + '-empty'">{{ 'common.noResults' | translate }}</td>
          </tr>
        </ng-template>
      </p-table>
    </p-dialog>
  `,
})
export class AuditHistoryDialogComponent {
  /** Prefiks id dla dialogu i elementów wewnętrznych (unikalny per widok wołający). */
  readonly dialogId = input.required<string>();
  readonly visible = input.required<boolean>();
  readonly events = input.required<AuditEvent[]>();
  readonly loading = input<boolean>(false);

  readonly visibleChange = output<boolean>();
}
