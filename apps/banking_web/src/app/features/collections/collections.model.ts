import { StatusSeverity } from '../../shared/components/status-tag.component';

export type CollectionsStage =
  'monitoring' | 'reminder' | 'settlement' | 'restructuring' | 'external' | 'legal';

export const COLLECTIONS_SEVERITY: Record<CollectionsStage, StatusSeverity> = {
  monitoring: 'info',
  reminder: 'warn',
  settlement: 'warn',
  restructuring: 'secondary',
  external: 'danger',
  legal: 'danger',
};

export interface CollectionsCase {
  id: string;
  customer: string;
  loanId: string;
  daysPastDue: number;
  outstandingAmount: number;
  stage: CollectionsStage;
}

/** GET /collections/{id}/actions — windykacyjne akcje (remind/proposeSettlement/transferExternal). */
export type CollectionsActionType = 'REMINDER_SENT' | 'SETTLEMENT_PROPOSED' | 'EXTERNAL_TRANSFER';

export interface CollectionsAction {
  actionType: CollectionsActionType;
  performedAt: string;
  performedBy: string;
  governanceAuditEventId: string | null;
}
