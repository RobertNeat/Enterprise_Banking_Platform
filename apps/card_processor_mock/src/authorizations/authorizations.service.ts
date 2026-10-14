import { Injectable, NotFoundException } from '@nestjs/common';
import { randomUUID, randomBytes } from 'node:crypto';
import type { CardAuthorization, CardNetwork } from './authorization.model.js';

export interface CreateAuthorizationInput {
  cardId: string;
  merchant: string;
  amount: number;
  mcc: string;
  processor?: CardNetwork;
}

const DECLINE_THRESHOLD = 5000;

@Injectable()
export class AuthorizationsService {
  private readonly authorizations = new Map<string, CardAuthorization>();

  create(input: CreateAuthorizationInput): CardAuthorization {
    const approved = input.amount <= DECLINE_THRESHOLD;
    const authorization: CardAuthorization = {
      id: randomUUID(),
      cardId: input.cardId,
      merchant: input.merchant,
      mcc: input.mcc,
      amount: input.amount,
      date: new Date().toISOString(),
      processor: input.processor ?? this.deriveProcessor(input.cardId),
      approved,
      responseCode: approved ? '00' : '05',
      processorId: `PRC-${randomBytes(4).toString('hex').toUpperCase()}`,
    };
    this.authorizations.set(authorization.id, authorization);
    return authorization;
  }

  findAll(cardId?: string): CardAuthorization[] {
    const all = Array.from(this.authorizations.values());
    return cardId ? all.filter((a) => a.cardId === cardId) : all;
  }

  findById(id: string): CardAuthorization {
    const authorization = this.authorizations.get(id);
    if (!authorization) {
      throw new NotFoundException(`Authorization ${id} not found`);
    }
    return authorization;
  }

  exists(id: string): boolean {
    return this.authorizations.has(id);
  }

  // Deterministic pseudo-random processor pick based on cardId, used when the
  // caller doesn't supply one explicitly.
  private deriveProcessor(cardId: string): CardNetwork {
    let hash = 0;
    for (let i = 0; i < cardId.length; i += 1) {
      hash = (hash * 31 + cardId.charCodeAt(i)) % 1000;
    }
    return hash % 2 === 0 ? 'Visa' : 'Mastercard';
  }
}
