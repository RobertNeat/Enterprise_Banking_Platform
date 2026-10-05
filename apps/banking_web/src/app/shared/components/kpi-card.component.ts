import { Component, input } from '@angular/core';
import { TranslatePipe } from '../../core/services/translate.pipe';

export type KpiTrend = 'up' | 'down' | 'flat';

@Component({
  selector: 'app-kpi-card',
  standalone: true,
  imports: [TranslatePipe],
  template: `
    <div class="kpi">
      <span class="kpi__label">{{ labelKey() | translate }}</span>
      <span class="kpi__value">{{ value() }}</span>
      @if (trendLabel()) {
        <span
          class="kpi__trend"
          [class.kpi__trend--up]="trend() === 'up'"
          [class.kpi__trend--down]="trend() === 'down'"
        >
          <i
            class="pi"
            [class.pi-arrow-up]="trend() === 'up'"
            [class.pi-arrow-down]="trend() === 'down'"
          ></i>
          {{ trendLabel() }}
        </span>
      }
    </div>
  `,
  styles: `
    .kpi {
      display: flex;
      flex-direction: column;
      gap: 0.35rem;
      padding: 1.1rem 1.25rem;
      border-radius: 12px;
      background: var(--p-content-background);
      border: 1px solid var(--p-content-border-color);
    }
    .kpi__label {
      font-size: 0.8rem;
      color: var(--p-text-muted-color);
      font-weight: 500;
    }
    .kpi__value {
      font-size: 1.6rem;
      font-weight: 700;
      color: var(--p-text-color);
    }
    .kpi__trend {
      font-size: 0.8rem;
      font-weight: 600;
      color: var(--p-text-muted-color);
      display: inline-flex;
      align-items: center;
      gap: 0.25rem;
      width: fit-content;
    }
    .kpi__trend--up {
      color: var(--p-green-600);
    }
    .kpi__trend--down {
      color: var(--p-red-600);
    }
  `,
})
export class KpiCardComponent {
  readonly labelKey = input.required<string>();
  readonly value = input.required<string>();
  readonly trend = input<KpiTrend>('flat');
  readonly trendLabel = input<string>('');
}
