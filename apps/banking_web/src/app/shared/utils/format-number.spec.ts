import { formatCount, formatMoney, formatPercent } from './format-number';

describe('format-number', () => {
  it('formatMoney always uses pl-PL separators regardless of UI language', () => {
    expect(formatMoney(15000)).toBe('15 000,00');
    expect(formatMoney(1500)).toBe('1 500,00');
    expect(formatMoney(1234567.5)).toBe('1 234 567,50');
  });

  it('formatCount groups 4-digit values the same as larger ones', () => {
    expect(formatCount(18420)).toBe('18 420');
    expect(formatCount(312)).toBe('312');
  });

  it('formatPercent uses a comma decimal separator and an NBSP before %', () => {
    expect(formatPercent(3.8)).toBe('3,8 %');
    expect(formatPercent(142, 0)).toBe('142 %');
  });
});
