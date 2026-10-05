import { StatusSeverity } from '../../shared/components/status-tag.component';

export type AccessStatus = 'active' | 'locked' | 'pendingReset';

export const ACCESS_SEVERITY: Record<AccessStatus, StatusSeverity> = {
  active: 'success',
  locked: 'danger',
  pendingReset: 'warn',
};

export interface EbankingUser {
  id: string;
  username: string;
  customer: string;
  channel: 'web' | 'mobile';
  accessStatus: AccessStatus;
  dailyLimit: number;
  lastLogin: string;
  permissions: string[];
}
