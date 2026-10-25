import { Component, effect, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastModule } from 'primeng/toast';
import { PrimeNG } from 'primeng/config';
import { TranslationService } from './core/services/translation.service';
import { PRIMENG_TRANSLATION_PL } from './core/services/primeng-locale';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastModule],
  template: `
    <p-toast
      id="app-toast"
      position="bottom-right"
      [pt]="{ closeButton: { id: 'app-toast-close-btn' } }"
    />
    <router-outlet />
  `,
})
export class App {
  private readonly primeng = inject(PrimeNG);
  private readonly i18n = inject(TranslationService);

  /**
   * PrimeNG's setTranslation() merges into the current translation object, it never clears keys —
   * switching back to 'en' after 'pl' would otherwise leave the Polish day/month names in place.
   * Snapshot the shipped English default once, before this effect ever calls setTranslation, so
   * 'en' can be restored exactly.
   */
  private readonly defaultTranslation = { ...this.primeng.translation };

  constructor() {
    // Kalendarz/datepicker PrimeNG nie zmienia języka wraz z resztą UI bez tego —
    // pakiet nie ma wbudowanych polskich nazw dni/miesięcy.
    effect(() => {
      this.primeng.setTranslation(
        this.i18n.lang() === 'pl' ? PRIMENG_TRANSLATION_PL : this.defaultTranslation,
      );
    });
  }
}
