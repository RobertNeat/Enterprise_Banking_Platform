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

// Fixed authorization ids matching apps/banking_backend's card_authorization_ref seed
// (db/changelog/seed/003-card.sql) so chargeback/card-status demo actions on seeded rows don't 404.
const SEED_AUTHORIZATIONS: CardAuthorization[] = [
  {
    id: 'dfd92a8d-976a-4601-97be-13ab2e09c7b1',
    cardId: 'CRD-5521',
    merchant: 'Empik',
    mcc: '5942',
    amount: 214.5,
    date: '2026-09-20T10:15:00.000Z',
    processor: 'Visa',
    approved: true,
    responseCode: '00',
    processorId: 'PRC-SEED0001',
  },
  {
    id: 'fc936c4c-ffaf-4dd1-9fdd-c5b4af746a98',
    cardId: 'CRD-5521',
    merchant: 'Orlen',
    mcc: '5541',
    amount: 180.0,
    date: '2026-09-22T08:05:00.000Z',
    processor: 'Visa',
    approved: true,
    responseCode: '00',
    processorId: 'PRC-SEED0002',
  },
  {
    id: '449084f7-77b5-4cab-a790-e680763a6572',
    cardId: 'CRD-5530',
    merchant: 'Amazon EU',
    mcc: '5969',
    amount: 1340.2,
    date: '2026-09-25T19:40:00.000Z',
    processor: 'Visa',
    approved: true,
    responseCode: '00',
    processorId: 'PRC-SEED0003',
  },
  {
    id: 'c78bf1a6-6d59-4a54-8de4-f5a3bc862310',
    cardId: 'CRD-5522',
    merchant: 'Biedronka',
    mcc: '5411',
    amount: 96.4,
    date: '2026-09-18T17:25:00.000Z',
    processor: 'Mastercard',
    approved: true,
    responseCode: '00',
    processorId: 'PRC-SEED0004',
  },
];

@Injectable()
export class AuthorizationsService {
  private readonly authorizations = new Map<string, CardAuthorization>();

  constructor() {
    // Read at construction time (not module load) so tests can toggle the env var per-case
    // without needing to reset the module registry (no @nestjs/config in this project — rule 3).
    if (process.env.CARD_PROCESSOR_SEED_MOCK_DATA !== 'false') {
      for (const authorization of SEED_AUTHORIZATIONS) {
        this.authorizations.set(authorization.id, { ...authorization });
      }
    }
  }

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
