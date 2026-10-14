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

/**
 * POST /cases request — statutoryDeadline/slaRemainingHours are computed by banking_backend
 * (F8 rule, see CaseActionsService), not accepted from the wizard. ownerId is optional; the
 * backend defaults it to the JWT caller when omitted.
 */
export interface CreateCustomerCaseRequest {
  subject: string;
  customerId: string;
  ownerId?: string;
  priority: 'low' | 'medium' | 'high';
  caseType: string;
}

export interface EscalateCaseResponse {
  customerCase: CustomerCase;
  escalated: boolean;
}
