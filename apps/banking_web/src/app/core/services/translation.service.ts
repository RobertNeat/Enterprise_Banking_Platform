import { Injectable, computed, signal } from '@angular/core';
import { en } from './i18n/en';
import { pl } from './i18n/pl';

export type Lang = 'pl' | 'en';

const DICTS: Record<Lang, Record<string, string>> = { pl, en };
const STORAGE_KEY = 'bank.lang';

@Injectable({ providedIn: 'root' })
export class TranslationService {
  private readonly langSignal = signal<Lang>(this.detectInitialLang());

  readonly lang = this.langSignal.asReadonly();
  readonly dict = computed(() => DICTS[this.langSignal()]);

  setLang(lang: Lang): void {
    this.langSignal.set(lang);
    localStorage.setItem(STORAGE_KEY, lang);
    document.documentElement.lang = lang;
  }

  toggle(): void {
    this.setLang(this.langSignal() === 'pl' ? 'en' : 'pl');
  }

  translate(key: string, params?: Record<string, string | number>): string {
    let text = this.dict()[key] ?? key;
    if (params) {
      for (const [k, v] of Object.entries(params)) {
        text = text.replace(`{{${k}}}`, String(v));
      }
    }
    return text;
  }

  /** Domyślnie 'pl' niezależnie od przeglądarki — to polska platforma bankowa. */
  private detectInitialLang(): Lang {
    const stored = localStorage.getItem(STORAGE_KEY) as Lang | null;
    return stored === 'pl' || stored === 'en' ? stored : 'pl';
  }
}
