import { Pipe, PipeTransform } from '@angular/core';
import { formatPercent } from './format-number';

/** Zawsze pl-PL, niezależnie od języka UI (zob. format-number.ts) — pure pipe wystarcza. */
@Pipe({ name: 'pct' })
export class PercentPipe implements PipeTransform {
  transform(value: number | null | undefined, fractionDigits = 1): string {
    if (value === null || value === undefined) return '—';
    return formatPercent(value, fractionDigits);
  }
}
