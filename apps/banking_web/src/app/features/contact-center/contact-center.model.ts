import { StatusSeverity } from '../../shared/components/status-tag.component';

export interface ChatMessage {
  from: 'agent' | 'customer';
  text: string;
  time: string;
}

export interface ServiceDisposition {
  id: string;
  typeKey: string;
  statusKey: string;
  severity: StatusSeverity;
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
