import { StatusSeverity } from '../../shared/components/status-tag.component';

export type CollateralType = 'mortgage' | 'pledge' | 'guarantee';
export type Ifrs9Stage = 'stage1' | 'stage2' | 'stage3';

export const IFRS9_SEVERITY: Record<Ifrs9Stage, StatusSeverity> = {
  stage1: 'success',
  stage2: 'warn',
  stage3: 'danger',
};

export interface CollateralEntry {
  id: string;
  customer: string;
  type: CollateralType;
  valuation: number;
  ltv: number;
  rating: string;
  stage: Ifrs9Stage;
  provision: number;
}
