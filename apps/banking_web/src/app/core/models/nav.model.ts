export interface NavItem {
  labelKey: string;
  icon: string;
  route: string;
  /**
   * Permission key from identity_access_mock's RolePermissions that gates visibility in the
   * shell nav (hides the link only — routes are guarded for authentication, not permissions).
   * Undefined means "visible to any authenticated user": identity_access_mock has no matching
   * permission value for this page yet (see docs/services/banking_backend.md).
   */
  permission?: string;
}

export const NAV_ITEMS: NavItem[] = [
  {
    labelKey: 'nav.dashboard',
    icon: 'pi pi-chart-line',
    route: '/dashboard',
    permission: 'dashboard',
  },
  { labelKey: 'nav.loans', icon: 'pi pi-wallet', route: '/loans', permission: 'loans' },
  { labelKey: 'nav.cards', icon: 'pi pi-credit-card', route: '/cards', permission: 'cards' },
  { labelKey: 'nav.risk', icon: 'pi pi-shield', route: '/risk', permission: 'risk' },
  { labelKey: 'nav.ebanking', icon: 'pi pi-desktop', route: '/ebanking', permission: 'ebanking' },
  {
    labelKey: 'nav.contact',
    icon: 'pi pi-comments',
    route: '/contact-center',
    permission: 'contact',
  },
  {
    labelKey: 'nav.enforcement',
    icon: 'pi pi-gavel',
    route: '/enforcement',
    permission: 'enforcement',
  },
  // No "reporting" permission exists upstream — mapped to "compliance" (regulatory reporting is
  // that role's domain) so the Compliance/AML demo account isn't left with only the dashboard.
  {
    labelKey: 'nav.reporting',
    icon: 'pi pi-file-export',
    route: '/reporting',
    permission: 'compliance',
  },
  // No "cases" permission exists upstream at all — visible to any authenticated user for now.
  { labelKey: 'nav.cases', icon: 'pi pi-folder-open', route: '/cases' },
  {
    labelKey: 'nav.collections',
    icon: 'pi pi-exclamation-triangle',
    route: '/collections',
    permission: 'collections',
  },
  // "approvals" added in R1.5 specifically for this screen (admin/compliance only) — see
  // docs/identity_access_mock_contract/api.md. Button-level decide/reject visibility inside the
  // page is a SEPARATE, finer-grained check (seniority vs. the approval's requester), not just
  // this nav gate.
  {
    labelKey: 'nav.approvals',
    icon: 'pi pi-check-square',
    route: '/approvals',
    permission: 'approvals',
  },
  {
    labelKey: 'nav.accounts',
    icon: 'pi pi-users',
    route: '/accounts',
    permission: 'accounts-admin',
  },
];
