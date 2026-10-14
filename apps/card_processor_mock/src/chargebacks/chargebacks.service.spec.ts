import { ConflictException, NotFoundException } from '@nestjs/common';
import { ChargebacksService } from './chargebacks.service.js';

describe('ChargebacksService', () => {
  let service: ChargebacksService;

  beforeEach(() => {
    service = new ChargebacksService();
  });

  it('files a chargeback in status FILED', () => {
    const chargeback = service.create({
      authorizationId: 'auth-1',
      reasonCode: '4855',
      description: 'Goods not delivered',
    });
    expect(chargeback.status).toBe('FILED');
  });

  it('advances FILED -> UNDER_REVIEW -> RESOLVED_CUSTOMER', () => {
    const chargeback = service.create({
      authorizationId: 'auth-1',
      reasonCode: '4855',
      description: 'Goods not delivered',
    });
    service.advance(chargeback.id, 'UNDER_REVIEW');
    const resolved = service.advance(chargeback.id, 'RESOLVED_CUSTOMER');
    expect(resolved.status).toBe('RESOLVED_CUSTOMER');
  });

  it('rejects skipping UNDER_REVIEW', () => {
    const chargeback = service.create({
      authorizationId: 'auth-1',
      reasonCode: '4855',
      description: 'Goods not delivered',
    });
    expect(() => service.advance(chargeback.id, 'RESOLVED_MERCHANT')).toThrow(
      ConflictException,
    );
  });

  it('rejects advancing from a resolved state', () => {
    const chargeback = service.create({
      authorizationId: 'auth-1',
      reasonCode: '4855',
      description: 'Goods not delivered',
    });
    service.advance(chargeback.id, 'UNDER_REVIEW');
    service.advance(chargeback.id, 'RESOLVED_MERCHANT');
    expect(() => service.advance(chargeback.id, 'UNDER_REVIEW')).toThrow(
      ConflictException,
    );
  });

  it('throws NotFoundException for an unknown chargeback', () => {
    expect(() => service.findById('missing')).toThrow(NotFoundException);
  });

  it('filters chargebacks by authorizationId', () => {
    service.create({
      authorizationId: 'auth-1',
      reasonCode: '4855',
      description: 'x',
    });
    service.create({
      authorizationId: 'auth-2',
      reasonCode: '4837',
      description: 'y',
    });
    expect(service.findAll('auth-1')).toHaveLength(1);
    expect(service.findAll()).toHaveLength(2);
  });
});
