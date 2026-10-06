import { CaseStatus } from '../cases/cases.model';

export interface ChatMessage {
  from: 'agent' | 'customer';
  text: string;
  time: string;
}

// Dyspozycja jest zawsze powiązana z konkretną sprawą (linkedCase), więc jej
// status żyje w tej samej domenie co CaseStatus/CASE_SEVERITY.
export interface ServiceDisposition {
  id: string;
  typeKey: string;
  status: CaseStatus;
  linkedCase: string;
  date: string;
}

export interface ContactHistoryEntry {
  id: string;
  channelKey: string;
  date: string;
  summary: string;
}

export interface CustomerProfile {
  name: string;
  segment: string;
  loansCount: number;
  cardsCount: number;
  riskRating: string;
  openCases: number;
}
