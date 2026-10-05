import { StatusSeverity } from '../../shared/components/status-tag.component';

export type CardStatus = 'active' | 'blocked' | 'pending' | 'expired';
export type CardNetwork = 'Visa' | 'Mastercard';

export const CARD_STATUS_SEVERITY: Record<CardStatus, StatusSeverity> = {
  active: 'success',
  blocked: 'danger',
  pending: 'info',
  expired: 'secondary',
};

export interface PaymentCard {
  id: string;
  holder: string;
  maskedNumber: string;
  network: CardNetwork;
  product: string;
  status: CardStatus;
  expiry: string;
  limitDaily: number;
  limitAtm: number;
}

export interface CardAuthorization {
  id: string;
  cardId: string;
  merchant: string;
  amount: number;
  date: string;
  processor: CardNetwork;
  approved: boolean;
}
