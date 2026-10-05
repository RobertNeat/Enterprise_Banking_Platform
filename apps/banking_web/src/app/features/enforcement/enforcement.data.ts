import { EnforcementCase } from './enforcement.model';

export const ENFORCEMENT_CASES: EnforcementCase[] = [
  {
    id: 'EN-2201',
    customer: 'Maria Lis',
    bailiff: 'Komornik Sądowy A. Dąbrowski',
    caseNumber: 'KM 1123/26',
    blockedAmount: 18400,
    transferredAmount: 6200,
    protectedAmount: 1631.4,
    status: 'active',
  },
  {
    id: 'EN-2198',
    customer: 'Piotr Kaczmarek',
    bailiff: 'Komornik Sądowy M. Wójcik',
    caseNumber: 'KM 998/26',
    blockedAmount: 4200,
    transferredAmount: 4200,
    protectedAmount: 1631.4,
    status: 'settled',
  },
  {
    id: 'EN-2190',
    customer: 'Nowak Logistyka Sp. z o.o.',
    bailiff: 'Komornik Sądowy T. Nowicki',
    caseNumber: 'KM 887/25',
    blockedAmount: 128000,
    transferredAmount: 128000,
    protectedAmount: 0,
    status: 'closed',
  },
  {
    id: 'EN-2205',
    customer: 'Katarzyna Zając',
    bailiff: 'Komornik Sądowy A. Dąbrowski',
    caseNumber: 'KM 1145/26',
    blockedAmount: 0,
    transferredAmount: 0,
    protectedAmount: 1631.4,
    status: 'registered',
  },
];
