import { Pipe, PipeTransform } from '@angular/core';
import { formatMoney } from './format-number';

/** Zawsze pl-PL, niezależnie od języka UI (zob. format-number.ts) — pure pipe wystarcza. */
@Pipe({ name: 'money' })
export class MoneyPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return formatMoney(value);
  }
}
