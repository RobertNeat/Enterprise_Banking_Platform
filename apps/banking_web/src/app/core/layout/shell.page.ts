import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { TranslatePipe } from '../services/translate.pipe';
import { TranslationService } from '../services/translation.service';
import { ThemeService } from '../services/theme.service';
import { NAV_ITEMS } from '../models/nav.model';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    ButtonModule,
    ToastModule,
    TooltipModule,
    TranslatePipe,
  ],
  templateUrl: './shell.page.html',
  styleUrl: './shell.page.css',
})
export class ShellPage {
  protected readonly i18n = inject(TranslationService);
  protected readonly theme = inject(ThemeService);
  protected readonly navItems = NAV_ITEMS;
}
