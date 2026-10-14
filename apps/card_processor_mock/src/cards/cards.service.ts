import { Injectable } from '@nestjs/common';

// Tracks only the block flag per cardId. The full PaymentCard entity lives in
// banking_backend/frontend — this service never owns it.
@Injectable()
export class CardsService {
  private readonly blockedCards = new Set<string>();

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
