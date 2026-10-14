// Visa/Mastercard reason codes relevant to the mocked chargeback registry.
export type ChargebackReasonCode = '4855' | '4837' | '4853' | '4863';

export type ChargebackStatus =
  'FILED' | 'UNDER_REVIEW' | 'RESOLVED_CUSTOMER' | 'RESOLVED_MERCHANT';

export interface Chargeback {
  id: string;
  authorizationId: string;
  reasonCode: ChargebackReasonCode;
  description: string;
  status: ChargebackStatus;
  filedAt: string;
}
