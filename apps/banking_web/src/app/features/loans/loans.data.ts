import { Installment, LoanApplication } from './loans.model';

export const LOAN_APPLICATIONS: LoanApplication[] = [
  {
    id: 'LN-80231',
    applicant: 'Jan Wiśniewski',
    segment: 'retail',
    product: 'mortgage',
    stage: 'contract',
    requestedAmount: 480000,
    approvedAmount: 460000,
    scoreResult: 782,
    dsti: 0.34,
    createdAt: '2026-08-12',
  },
  {
    id: 'LN-80244',
    applicant: 'Katarzyna Zając',
    segment: 'retail',
    product: 'cash',
    stage: 'disbursement',
    requestedAmount: 35000,
    approvedAmount: 35000,
    scoreResult: 701,
    dsti: 0.28,
    createdAt: '2026-09-01',
  },
  {
    id: 'LN-80255',
    applicant: 'Agro-Bud S.A.',
    segment: 'business',
    product: 'investment',
    stage: 'decision',
    requestedAmount: 2200000,
    approvedAmount: null,
    scoreResult: 655,
    dsti: 0.41,
    createdAt: '2026-09-18',
  },
  {
    id: 'LN-80261',
    applicant: 'Piotr Kaczmarek',
    segment: 'retail',
    product: 'overdraft',
    stage: 'schedule',
    requestedAmount: 8000,
    approvedAmount: 8000,
    scoreResult: 744,
    dsti: 0.22,
    createdAt: '2026-06-02',
  },
  {
    id: 'LN-80270',
    applicant: 'Nowak Logistyka Sp. z o.o.',
    segment: 'business',
    product: 'creditLine',
    stage: 'scoring',
    requestedAmount: 950000,
    approvedAmount: null,
    scoreResult: 612,
    dsti: 0.47,
    createdAt: '2026-09-29',
  },
  {
    id: 'LN-80203',
    applicant: 'Maria Lis',
    segment: 'retail',
    product: 'cash',
    stage: 'collections',
    requestedAmount: 22000,
    approvedAmount: 22000,
    scoreResult: 589,
    dsti: 0.52,
    createdAt: '2026-03-15',
  },
  {
    id: 'LN-80288',
    applicant: 'BetaTech S.A.',
    segment: 'business',
    product: 'investment',
    stage: 'application',
    requestedAmount: 4100000,
    approvedAmount: null,
    scoreResult: 0,
    dsti: 0,
    createdAt: '2026-10-02',
  },
  {
    id: 'LN-80219',
    applicant: 'Anna Kowalska',
    segment: 'retail',
    product: 'mortgage',
    stage: 'affordability',
    requestedAmount: 620000,
    approvedAmount: null,
    scoreResult: 697,
    dsti: 0.39,
    createdAt: '2026-09-20',
  },
];

export function buildSchedule(
  totalAmount: number,
  months: number,
  annualRate = 0.079,
): Installment[] {
  const monthlyPrincipal = totalAmount / months;
  let remaining = totalAmount;
  const today = new Date('2026-10-05');
  const schedule: Installment[] = [];
  for (let i = 1; i <= months; i++) {
    const interest = remaining * (annualRate / 12);
    const due = new Date(today);
    due.setMonth(due.getMonth() - 6 + i);
    const status: Installment['status'] =
      due < today ? (i % 7 === 0 ? 'overdue' : 'paid') : 'upcoming';
    schedule.push({
      no: i,
      dueDate: due.toISOString().slice(0, 10),
      principal: Math.round(monthlyPrincipal),
      interest: Math.round(interest),
      status,
    });
    remaining -= monthlyPrincipal;
  }
  return schedule;
}
