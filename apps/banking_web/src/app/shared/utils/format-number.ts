/**
 * Zawsze pl-PL, niezależnie od przełącznika języka UI — żądanie użytkownika było bezwarunkowe
 * (polskie wartości pieniężne muszą używać polskiej konwencji, nawet gdy etykiety są po angielsku).
 * Separator tysięcy U+00A0 (NBSP) w pl-PL — CLDR dla tej lokalizacji domyślnie ustawia
 * minimumGroupingDigits=2 (np. 4500 bez separatora), stąd useGrouping:'always' wymuszone
 * poniżej, żeby kwoty 4-cyfrowe grupowały się tak samo jak większe.
 */
const LOCALE = 'pl-PL';

/**
 * TS lib's Intl.NumberFormatOptions (ES2022 target) only types useGrouping as boolean — the
 * string values ('always'/'auto'/'min2') are real at runtime (ECMA-402) but need a cast here.
 */
export type GroupingOptions = Omit<Intl.NumberFormatOptions, 'useGrouping'> & {
  useGrouping?: boolean | 'always';
};

export function numberFormat(options: GroupingOptions): Intl.NumberFormat {
  return new Intl.NumberFormat(LOCALE, options as unknown as Intl.NumberFormatOptions);
}

/** Kwota pieniężna z 2 miejscami po przecinku, zawsze zgrupowana. */
export function formatMoney(value: number): string {
  return numberFormat({
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
    useGrouping: 'always',
  }).format(value);
}

/** Liczba całkowita (np. licznik), zawsze zgrupowana. */
export function formatCount(value: number): string {
  return numberFormat({ useGrouping: 'always' }).format(value);
}

const NBSP = String.fromCharCode(0x00a0);

/** Procent z NBSP przed znakiem '%' (polska typografia) — np. "12,3 %". */
export function formatPercent(value: number, fractionDigits = 1): string {
  const formatted = numberFormat({
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  }).format(value);
  return `${formatted}${NBSP}%`;
}
