import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MenuItem } from 'primeng/api';
import { SplitButtonModule } from 'primeng/splitbutton';
import { TranslatePipe } from '../services/translate.pipe';
import { TranslationService } from '../services/translation.service';
import { ThemeService } from '../services/theme.service';
import { NAV_ITEMS } from '../models/nav.model';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, SplitButtonModule, TranslatePipe],
  templateUrl: './shell.page.html',
  styleUrl: './shell.page.css',
})
export class ShellPage {
  protected readonly i18n = inject(TranslationService);
  protected readonly theme = inject(ThemeService);
  private readonly router = inject(Router);
  protected readonly navItems = NAV_ITEMS;

  protected readonly userMenuLabel = computed(() => this.i18n.translate('user.mockName'));

  protected readonly userMenuItems = computed<MenuItem[]>(() => {
    const isDark = this.theme.mode() === 'dark';
    return [
      {
        id: 'app-user-menu-theme',
        label: this.i18n.translate(isDark ? 'user.menu.theme.light' : 'user.menu.theme.dark'),
        icon: isDark ? 'pi pi-sun' : 'pi pi-moon',
        command: () => this.theme.toggle(),
      },
      {
        id: 'app-user-menu-lang',
        label: this.i18n.translate('user.menu.lang'),
        icon: 'pi pi-language',
        command: () => this.i18n.toggle(),
      },
      {
        id: 'app-user-menu-separator',
        separator: true,
      },
      {
        id: 'app-user-menu-logout',
        label: this.i18n.translate('user.menu.logout'),
        icon: 'pi pi-sign-out',
        command: () => void this.router.navigate(['/login']),
      },
    ];
  });
}
