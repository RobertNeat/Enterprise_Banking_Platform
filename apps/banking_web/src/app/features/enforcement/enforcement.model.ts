import { StatusSeverity } from '../../shared/components/status-tag.component';

export type EnforcementStatus = 'registered' | 'active' | 'settled' | 'closed';
export type EnforcementKind = 'ALIMONY' | 'NON_ALIMONY';

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

/** POST /enforcement body — no transferredAmount: nothing is transferred yet at registration. */
export interface RegisterEnforcementRequest {
  bailiff: string;
  caseNumber: string;
  customer: string;
  kind: EnforcementKind;
  blockedAmount: number;
  protectedAmount?: number;
}

export interface RequestTransferApprovalResponse {
  enforcementCase: EnforcementCase;
  approvalId: string;
}
