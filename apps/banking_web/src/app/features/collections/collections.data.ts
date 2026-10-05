import { CollectionsCase } from './collections.model';

export const COLLECTIONS_CASES: CollectionsCase[] = [
  {
    id: 'DBT-6601',
    customer: 'Maria Lis',
    loanId: 'LN-80203',
    daysPastDue: 92,
    outstandingAmount: 22600,
    stage: 'legal',
  },
  {
    id: 'DBT-6598',
    customer: 'Robert Nowak',
    loanId: 'LN-80180',
    daysPastDue: 61,
    outstandingAmount: 14200,
    stage: 'external',
  },
  {
    id: 'DBT-6590',
    customer: 'Ewa Majewska',
    loanId: 'LN-80175',
    daysPastDue: 34,
    outstandingAmount: 5400,
    stage: 'settlement',
  },
  {
    id: 'DBT-6587',
    customer: 'Tomasz Kowal',
    loanId: 'LN-80162',
    daysPastDue: 15,
    outstandingAmount: 2100,
    stage: 'reminder',
  },
  {
    id: 'DBT-6580',
    customer: 'BetaTech S.A.',
    loanId: 'LN-80120',
    daysPastDue: 7,
    outstandingAmount: 48900,
    stage: 'monitoring',
  },
  {
    id: 'DBT-6572',
    customer: 'Anna Kowalska',
    loanId: 'LN-80099',
    daysPastDue: 48,
    outstandingAmount: 9800,
    stage: 'restructuring',
  },
];
