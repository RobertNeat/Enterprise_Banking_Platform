import { StatusSeverity } from '../../shared/components/status-tag.component';

export type LoanSegment = 'retail' | 'business';
export type LoanProduct = 'cash' | 'mortgage' | 'overdraft' | 'investment' | 'creditLine';
export type LoanStage =
  | 'application'
  | 'scoring'
  | 'affordability'
  | 'decision'
  | 'contract'
  | 'disbursement'
  | 'schedule'
  | 'collections';

export const LOAN_STAGES: LoanStage[] = [
  'application',
  'scoring',
  'affordability',
  'decision',
  'contract',
  'disbursement',
  'schedule',
  'collections',
];

export const LOAN_STAGE_SEVERITY: Record<LoanStage, StatusSeverity> = {
  application: 'info',
  scoring: 'info',
  affordability: 'info',
  decision: 'warn',
  contract: 'warn',
  disbursement: 'success',
  schedule: 'success',
  collections: 'danger',
};

export interface LoanApplication {
  id: string;
  applicant: string;
  segment: LoanSegment;
  product: LoanProduct;
  stage: LoanStage;
  requestedAmount: number;
  approvedAmount: number | null;
  scoreResult: number;
  dsti: number;
  createdAt: string;
}

export type InstallmentStatus = 'paid' | 'upcoming' | 'overdue';

export const INSTALLMENT_SEVERITY: Record<InstallmentStatus, StatusSeverity> = {
  paid: 'success',
  upcoming: 'info',
  overdue: 'danger',
};

export interface Installment {
  no: number;
  dueDate: string;
  principal: number;
  interest: number;
  status: InstallmentStatus;
}
