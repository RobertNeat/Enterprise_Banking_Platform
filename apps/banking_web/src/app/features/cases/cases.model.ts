import { StatusSeverity } from '../../shared/components/status-tag.component';

export type CaseStatus = 'new' | 'inProgress' | 'escalated' | 'resolved';

export const CASE_SEVERITY: Record<CaseStatus, StatusSeverity> = {
  new: 'info',
  inProgress: 'warn',
  escalated: 'danger',
  resolved: 'success',
};

export interface CustomerCase {
  id: string;
  subject: string;
  customer: string;
  owner: string;
  priority: 'low' | 'medium' | 'high';
  statutoryDeadline: string;
  slaRemainingHours: number;
  status: CaseStatus;
}
