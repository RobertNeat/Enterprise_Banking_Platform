import { RegulatoryReport } from './reporting.model';

export const REGULATORY_REPORTS: RegulatoryReport[] = [
  {
    id: 'RPT-KNF-001',
    recipient: 'KNF',
    name: 'Sprawozdanie adekwatności kapitałowej',
    period: 'Q3 2026',
    dueDate: '2026-10-15',
    status: 'draft',
  },
  {
    id: 'RPT-NBP-014',
    recipient: 'NBP',
    name: 'Sprawozdanie płynnościowe LCR/NSFR',
    period: 'Wrzesień 2026',
    dueDate: '2026-10-08',
    status: 'submitted',
  },
  {
    id: 'RPT-BFG-003',
    recipient: 'BFG',
    name: 'Raport składki gwarancyjnej',
    period: 'Q3 2026',
    dueDate: '2026-10-20',
    status: 'draft',
  },
  {
    id: 'RPT-FATCA-009',
    recipient: 'FATCA/CRS',
    name: 'Raport rachunków raportowanych',
    period: '2025',
    dueDate: '2026-06-30',
    status: 'accepted',
  },
  {
    id: 'RPT-KNF-002',
    recipient: 'KNF',
    name: 'Raport dużych ekspozycji',
    period: 'Q3 2026',
    dueDate: '2026-10-25',
    status: 'draft',
  },
];
