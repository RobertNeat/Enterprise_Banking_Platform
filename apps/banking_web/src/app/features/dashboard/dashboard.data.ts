import { StatusSeverity } from '../../shared/components/status-tag.component';
import { CaseStatus } from '../cases/cases.model';
import { ReportStatus } from '../reporting/reporting.model';

export interface RecentCase {
  id: string;
  subjectKey: string;
  customer: string;
  status: CaseStatus;
  owner: string;
}

export interface RegulatoryAlert {
  id: string;
  recipient: string;
  status: ReportStatus;
  dueDate: string;
}

export type IctIncidentSeverity = 'LOW' | 'MEDIUM' | 'HIGH';

export const ICT_INCIDENT_SEVERITY: Record<IctIncidentSeverity, StatusSeverity> = {
  LOW: 'info',
  MEDIUM: 'warn',
  HIGH: 'danger',
};

/** GET /dashboard/ict-incidents response shape — TOP N most recent OPEN incidents (DORA registry). */
export interface IctIncidentAlert {
  id: string;
  source: string;
  description: string;
  severity: IctIncidentSeverity;
  createdAt: string;
}

export const RECENT_CASES: RecentCase[] = [
  {
    id: 'CS-10231',
    subjectKey: 'cases.subject',
    customer: 'Nowak Sp. z o.o.',
    status: 'escalated',
    owner: 'A. Kowalska',
  },
  {
    id: 'CS-10229',
    subjectKey: 'cases.subject',
    customer: 'Jan Wiśniewski',
    status: 'inProgress',
    owner: 'P. Zieliński',
  },
  {
    id: 'CS-10225',
    subjectKey: 'cases.subject',
    customer: 'Agro-Bud S.A.',
    status: 'new',
    owner: 'M. Lis',
  },
  {
    id: 'CS-10219',
    subjectKey: 'cases.subject',
    customer: 'Katarzyna Zając',
    status: 'resolved',
    owner: 'A. Kowalska',
  },
];

// Te same rekordy (po recipient + dueDate) co RPT-KNF-001 / RPT-BFG-003 w
// reporting.data.ts — status pochodzi z tej samej domeny raportowej, żeby
// kolor zawsze zgadzał się z widokiem Reporting (REPORT_SEVERITY).
export const REGULATORY_ALERTS: RegulatoryAlert[] = [
  {
    id: 'REG-KNF-Q3',
    recipient: 'KNF',
    status: 'draft',
    dueDate: '2026-10-15',
  },
  {
    id: 'REG-NBP-09',
    recipient: 'NBP',
    status: 'submitted',
    dueDate: '2026-10-08',
  },
  {
    id: 'REG-BFG-Q3',
    recipient: 'BFG',
    status: 'draft',
    dueDate: '2026-10-20',
  },
];

/** GET /dashboard/kpi-snapshot/latest response shape — 404 when no snapshot is seeded. */
export interface KpiSnapshot {
  snapshotDate: string;
  activeLoansCount: number;
  loanPortfolioTotal: number;
  cardsIssuedCount: number;
  nplRatio: number;
  lcrRatio: number;
  nsfrRatio: number;
  openCasesCount: number;
  collectionsOverdueTotal: number;
}

/** GET /dashboard/portfolio-trend response shape — all points, sorted by snapshotDate asc server-side. */
export interface PortfolioTrendPoint {
  snapshotDate: string;
  portfolioTotal: number;
}

export const KPI_SNAPSHOT: KpiSnapshot = {
  snapshotDate: '2026-10-05',
  activeLoansCount: 18420,
  loanPortfolioTotal: 512400000.0,
  cardsIssuedCount: 64910,
  nplRatio: 3.8,
  lcrRatio: 142.0,
  nsfrRatio: 118.0,
  openCasesCount: 312,
  collectionsOverdueTotal: 24600000.0,
};

export const PORTFOLIO_TREND: PortfolioTrendPoint[] = [
  { snapshotDate: '2026-01-05', portfolioTotal: 412000000.0 },
  { snapshotDate: '2026-02-05', portfolioTotal: 428000000.0 },
  { snapshotDate: '2026-03-05', portfolioTotal: 441000000.0 },
  { snapshotDate: '2026-04-05', portfolioTotal: 455000000.0 },
  { snapshotDate: '2026-05-05', portfolioTotal: 468000000.0 },
  { snapshotDate: '2026-06-05', portfolioTotal: 479000000.0 },
  { snapshotDate: '2026-07-05', portfolioTotal: 491000000.0 },
  { snapshotDate: '2026-08-05', portfolioTotal: 503000000.0 },
  { snapshotDate: '2026-09-05', portfolioTotal: 512000000.0 },
];

export const ICT_INCIDENTS: IctIncidentAlert[] = [
  {
    id: 'inc-1',
    source: 'CARD_PROCESSOR',
    description: 'card_processor_mock unavailable podczas blokady karty',
    severity: 'HIGH',
    createdAt: '2026-10-18T10:00:00Z',
  },
  {
    id: 'inc-2',
    source: 'EXTERNAL_INSTITUTIONS_GATEWAY',
    description: 'external_institutions_gateway_mock unavailable podczas rejestracji zajęcia',
    severity: 'MEDIUM',
    createdAt: '2026-10-15T09:00:00Z',
  },
];
