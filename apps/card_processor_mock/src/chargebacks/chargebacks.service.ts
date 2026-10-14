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

@Injectable()
export class ChargebacksService {
  private readonly chargebacks = new Map<string, Chargeback>();

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
