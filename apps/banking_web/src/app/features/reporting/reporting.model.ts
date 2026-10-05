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
