export interface NavItem {
  labelKey: string;
  icon: string;
  route: string;
}

export const NAV_ITEMS: NavItem[] = [
  { labelKey: 'nav.dashboard', icon: 'pi pi-chart-line', route: '/dashboard' },
  { labelKey: 'nav.loans', icon: 'pi pi-wallet', route: '/loans' },
  { labelKey: 'nav.cards', icon: 'pi pi-credit-card', route: '/cards' },
  { labelKey: 'nav.risk', icon: 'pi pi-shield', route: '/risk' },
  { labelKey: 'nav.ebanking', icon: 'pi pi-desktop', route: '/ebanking' },
  { labelKey: 'nav.contact', icon: 'pi pi-comments', route: '/contact-center' },
  { labelKey: 'nav.enforcement', icon: 'pi pi-gavel', route: '/enforcement' },
  { labelKey: 'nav.reporting', icon: 'pi pi-file-export', route: '/reporting' },
  { labelKey: 'nav.cases', icon: 'pi pi-folder-open', route: '/cases' },
  { labelKey: 'nav.collections', icon: 'pi pi-exclamation-triangle', route: '/collections' },
];
