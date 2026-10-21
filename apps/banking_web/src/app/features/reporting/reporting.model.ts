import { StatusSeverity } from '../../shared/components/status-tag.component';

export type ReportStatus = 'draft' | 'submitted' | 'accepted';

export const REPORT_SEVERITY: Record<ReportStatus, StatusSeverity> = {
  draft: 'secondary',
  submitted: 'info',
  accepted: 'success',
};

export interface RegulatoryReport {
  id: string;
  recipient: 'KNF' | 'NBP' | 'BFG' | 'FATCA/CRS';
  name: string;
  period: string;
  dueDate: string;
  status: ReportStatus;
}

export type GatewaySubmissionStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED';

export const GATEWAY_SUBMISSION_SEVERITY: Record<GatewaySubmissionStatus, StatusSeverity> = {
  PENDING: 'info',
  ACCEPTED: 'success',
  REJECTED: 'danger',
};

export interface ReportSubmission {
  gatewaySubmissionId: string;
  gatewayStatus: GatewaySubmissionStatus;
  validationErrors: string | null;
  submittedAt: string;
  resolvedAt: string | null;
}

/** POST /reporting/reports/{id}/refresh-submission-status response. */
export interface RefreshSubmissionStatusResponse {
  report: RegulatoryReport;
  submission: ReportSubmission;
}
