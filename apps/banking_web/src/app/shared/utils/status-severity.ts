import { StatusSeverity } from '../components/status-tag.component';

/**
 * Severity dla etykiet kategoryzujących, które NIE są statusem biznesowym
 * (np. kanał e-bankowości, sieć/processor karty, uprawnienie użytkownika).
 * Jeden neutralny odcień dla całej appki — cel to spójny wygląd kontenera,
 * nie wymuszanie semantyki success/danger na nie-statusach.
 */
export const NEUTRAL_TAG_SEVERITY: StatusSeverity = 'secondary';

/**
 * Mapy semantic→severity per domena biznesowa żyją przy swoim modelu
 * (np. CASE_SEVERITY w cases.model.ts, CARD_STATUS_SEVERITY w cards.model.ts)
 * — ten plik jest punktem odniesienia dla wartości współdzielonych między
 * domenami (neutralne etykiety, priorytet) oraz re-exportem typu
 * StatusSeverity, żeby komponenty stron nie musiały importować go
 * z komponentu współdzielonego.
 */
export type { StatusSeverity };

export type Priority = 'low' | 'medium' | 'high';

/** Priorytet sprawy/zgłoszenia — ten sam skalowanie używane w kilku modułach. */
export const PRIORITY_SEVERITY: Record<Priority, StatusSeverity> = {
  low: 'secondary',
  medium: 'warn',
  high: 'danger',
};
