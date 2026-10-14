import { StatusSeverity } from '../../shared/components/status-tag.component';

export type LoanSegment = 'retail' | 'business';
export type LoanProduct = 'cash' | 'mortgage' | 'overdraft' | 'investment' | 'creditLine';

export const LOAN_PRODUCTS: LoanProduct[] = [
  'cash',
  'mortgage',
  'overdraft',
  'investment',
  'creditLine',
];
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

/** POST /loans request — scoreResult/dsti are no longer user input, see banking_backend's CreateLoanApplicationRequest. */
export interface CreateLoanApplicationRequest {
  customerId: string;
  segment: LoanSegment;
  product: LoanProduct;
  requestedAmount: number;
  termMonths?: number;
}

/** Full detail shape returned by POST /loans, POST /loans/{id}/advance-stage and GET /loans/{id}. */
export interface LoanDetail extends LoanApplication {
  interestRate: number;
  termMonths: number;
  installments: Installment[];
  stageTransitions: LoanStageTransitionEntry[];
}

export interface LoanStageTransitionEntry {
  fromStage: string;
  toStage: string;
  transitionedAt: string;
  transitionedBy: string;
  approvalId: string | null;
}

export interface SubmitForApprovalResponse {
  loan: LoanDetail;
  approvalId: string;
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
