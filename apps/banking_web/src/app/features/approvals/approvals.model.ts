import { StatusSeverity } from '../../shared/components/status-tag.component';

export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export const APPROVAL_STATUS_SEVERITY: Record<ApprovalStatus, StatusSeverity> = {
  PENDING: 'warn',
  APPROVED: 'success',
  REJECTED: 'danger',
};

/**
 * Mirrors banking_backend's ApprovalRecord (proxy to governance_mock's /approvals), including the
 * requester role/seniority fields banking_backend adds so the UI can gate the decide/reject
 * buttons without re-resolving employeeId -> role itself (single place for that mapping — see
 * docs/services/banking_backend.md).
 */
export interface Approval {
  id: string;
  type: string;
  requestedBy: string;
  payload: string | null;
  status: ApprovalStatus;
  createdAt: string;
  decidedBy: string | null;
  decidedAt: string | null;
  requestedByRole: string | null;
  requestedBySeniorityLevel: number | null;
}
