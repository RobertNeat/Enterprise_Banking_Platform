import { StatusSeverity } from '../../shared/components/status-tag.component';

export type AccountStatus = 'ACTIVE' | 'PENDING_APPROVAL' | 'REJECTED';

export const ACCOUNT_STATUS_SEVERITY: Record<AccountStatus, StatusSeverity> = {
  ACTIVE: 'success',
  PENDING_APPROVAL: 'warn',
  REJECTED: 'danger',
};

/** Mirrors identity_access_mock's AccountView, proxied 1:1 by banking_backend's /auth/accounts*. */
export interface EmployeeAccountView {
  id: string;
  employeeId: string;
  role: string | null;
  status: AccountStatus;
  firstName: string | null;
  lastName: string | null;
  email: string | null;
}

/**
 * Must match the Polish display names in identity_access_mock's RolePermissions — an unmapped
 * value makes /auth/accounts/{id}/approve fail with 400 there.
 */
export const ASSIGNABLE_ROLES: string[] = [
  'Administrator systemu',
  'Compliance / AML',
  'Doradca kredytowy',
  'Analityk ryzyka',
  'Windykator',
  'Konsultant kanału kontaktu',
];
