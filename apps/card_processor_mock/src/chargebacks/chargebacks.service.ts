import {
  ConflictException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { randomUUID } from 'node:crypto';
import type {
  Chargeback,
  ChargebackReasonCode,
  ChargebackStatus,
} from './chargeback.model.js';

export interface CreateChargebackInput {
  authorizationId: string;
  reasonCode: ChargebackReasonCode;
  description: string;
}

// Allowed transitions for the chargeback dispute cycle: FILED -> UNDER_REVIEW -> RESOLVED_*.
const ALLOWED_TRANSITIONS: Record<ChargebackStatus, ChargebackStatus[]> = {
  FILED: ['UNDER_REVIEW'],
  UNDER_REVIEW: ['RESOLVED_CUSTOMER', 'RESOLVED_MERCHANT'],
  RESOLVED_CUSTOMER: [],
  RESOLVED_MERCHANT: [],
};

// Matches apps/banking_backend's chargeback_ref seed (db/changelog/seed/003-card.sql): filed
// against authorization 449084f7-... (Amazon EU, CRD-5530), reasonCode 4837, status FILED.
const SEED_CHARGEBACKS: Chargeback[] = [
  {
    id: 'd9a633a4-1223-48cd-b470-217a69610114',
    authorizationId: '449084f7-77b5-4cab-a790-e680763a6572',
    reasonCode: '4837',
    description: 'Transakcja nieautoryzowana (karta nieobecna)',
    status: 'FILED',
    filedAt: '2026-09-26T09:00:00.000Z',
  },
];

@Injectable()
export class ChargebacksService {
  private readonly chargebacks = new Map<string, Chargeback>();

  constructor() {
    if (process.env.CARD_PROCESSOR_SEED_MOCK_DATA !== 'false') {
      for (const chargeback of SEED_CHARGEBACKS) {
        this.chargebacks.set(chargeback.id, { ...chargeback });
      }
    }
  }

  create(input: CreateChargebackInput): Chargeback {
    const chargeback: Chargeback = {
      id: randomUUID(),
      authorizationId: input.authorizationId,
      reasonCode: input.reasonCode,
      description: input.description,
      status: 'FILED',
      filedAt: new Date().toISOString(),
    };
    this.chargebacks.set(chargeback.id, chargeback);
    return chargeback;
  }

  findAll(authorizationId?: string): Chargeback[] {
    const all = Array.from(this.chargebacks.values());
    return authorizationId
      ? all.filter((c) => c.authorizationId === authorizationId)
      : all;
  }

  findById(id: string): Chargeback {
    const chargeback = this.chargebacks.get(id);
    if (!chargeback) {
      throw new NotFoundException(`Chargeback ${id} not found`);
    }
    return chargeback;
  }

  advance(id: string, nextStatus: ChargebackStatus): Chargeback {
    const chargeback = this.findById(id);
    const allowed = ALLOWED_TRANSITIONS[chargeback.status];
    if (!allowed.includes(nextStatus)) {
      throw new ConflictException(
        `Cannot transition chargeback from ${chargeback.status} to ${nextStatus}`,
      );
    }
    chargeback.status = nextStatus;
    return chargeback;
  }
}
