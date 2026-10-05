import { StatusSeverity } from '../../shared/components/status-tag.component';

export type EnforcementStatus = 'registered' | 'active' | 'settled' | 'closed';

export const ENFORCEMENT_SEVERITY: Record<EnforcementStatus, StatusSeverity> = {
  registered: 'info',
  active: 'danger',
  settled: 'warn',
  closed: 'success',
};

export interface EnforcementCase {
  id: string;
  customer: string;
  bailiff: string;
  caseNumber: string;
  blockedAmount: number;
  transferredAmount: number;
  protectedAmount: number;
  status: EnforcementStatus;
}
