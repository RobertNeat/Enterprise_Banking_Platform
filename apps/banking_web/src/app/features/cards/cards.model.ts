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

export type ChargebackReasonCode = '4855' | '4837' | '4853' | '4863';
export type ChargebackStatus = 'FILED' | 'UNDER_REVIEW' | 'RESOLVED_CUSTOMER' | 'RESOLVED_MERCHANT';

export const CHARGEBACK_REASON_CODES: ChargebackReasonCode[] = ['4855', '4837', '4853', '4863'];

export const CHARGEBACK_STATUS_SEVERITY: Record<ChargebackStatus, StatusSeverity> = {
  FILED: 'info',
  UNDER_REVIEW: 'warn',
  RESOLVED_CUSTOMER: 'success',
  RESOLVED_MERCHANT: 'success',
};

export interface Chargeback {
  id: string;
  cardAuthorizationId: string;
  reasonCode: ChargebackReasonCode;
  status: ChargebackStatus;
  filedAt: string;
}

export interface FileChargebackRequest {
  authorizationId: string;
  reasonCode: ChargebackReasonCode;
  description: string;
}
