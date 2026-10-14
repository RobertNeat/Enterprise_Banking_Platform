import { Injectable } from '@nestjs/common';

// Matches apps/banking_backend's payment_card seed (db/changelog/seed/003-card.sql): CRD-5522
// (CUST-002, Mastercard World) is seeded there with status='blocked'.
const SEED_BLOCKED_CARDS = ['CRD-5522'];

// Tracks only the block flag per cardId. The full PaymentCard entity lives in
// banking_backend/frontend — this service never owns it.
@Injectable()
export class CardsService {
  private readonly blockedCards = new Set<string>();

  constructor() {
    if (process.env.CARD_PROCESSOR_SEED_MOCK_DATA !== 'false') {
      for (const cardId of SEED_BLOCKED_CARDS) {
        this.blockedCards.add(cardId);
      }
    }
  }

  block(cardId: string): void {
    this.blockedCards.add(cardId);
  }

  unblock(cardId: string): void {
    this.blockedCards.delete(cardId);
  }

  isBlocked(cardId: string): boolean {
    return this.blockedCards.has(cardId);
  }
}
