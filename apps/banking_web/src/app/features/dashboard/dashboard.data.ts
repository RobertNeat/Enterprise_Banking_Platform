import { StatusSeverity } from '../../shared/components/status-tag.component';

export interface RecentCase {
  id: string;
  subjectKey: string;
  customer: string;
  statusKey: string;
  severity: StatusSeverity;
  owner: string;
}

export interface RegulatoryAlert {
  id: string;
  recipient: string;
  descriptionKey: string;
  dueDate: string;
  severity: StatusSeverity;
}

export const RECENT_CASES: RecentCase[] = [
  {
    id: 'CS-10231',
    subjectKey: 'cases.subject',
    customer: 'Nowak Sp. z o.o.',
    statusKey: 'cases.status.escalated',
    severity: 'danger',
    owner: 'A. Kowalska',
  },
  {
    id: 'CS-10229',
    subjectKey: 'cases.subject',
    customer: 'Jan Wiśniewski',
    statusKey: 'cases.status.inProgress',
    severity: 'warn',
    owner: 'P. Zieliński',
  },
  {
    id: 'CS-10225',
    subjectKey: 'cases.subject',
    customer: 'Agro-Bud S.A.',
    statusKey: 'cases.status.new',
    severity: 'info',
    owner: 'M. Lis',
  },
  {
    id: 'CS-10219',
    subjectKey: 'cases.subject',
    customer: 'Katarzyna Zając',
    statusKey: 'cases.status.resolved',
    severity: 'success',
    owner: 'A. Kowalska',
  },
];

export const REGULATORY_ALERTS: RegulatoryAlert[] = [
  {
    id: 'REG-KNF-Q3',
    recipient: 'KNF',
    descriptionKey: 'reporting.status.draft',
    dueDate: '2026-10-15',
    severity: 'warn',
  },
  {
    id: 'REG-NBP-09',
    recipient: 'NBP',
    descriptionKey: 'reporting.status.submitted',
    dueDate: '2026-10-08',
    severity: 'info',
  },
  {
    id: 'REG-BFG-Q3',
    recipient: 'BFG',
    descriptionKey: 'reporting.status.draft',
    dueDate: '2026-10-20',
    severity: 'danger',
  },
];
