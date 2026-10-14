export type CardNetwork = 'Visa' | 'Mastercard';

// Base fields match the frontend CardAuthorization contract; mcc/responseCode/processorId
// extend it with ISO 8583-style convention fields owned by this mock.
export interface CardAuthorization {
  id: string;
  cardId: string;
  merchant: string;
  mcc: string;
  amount: number;
  date: string;
  processor: CardNetwork;
  approved: boolean;
  responseCode: string;
  processorId: string;
}
