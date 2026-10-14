import { BadRequestException, NotFoundException } from '@nestjs/common';
import { ChargebacksController } from './chargebacks.controller.js';
import { ChargebacksService } from './chargebacks.service.js';
import { AuthorizationsService } from '../authorizations/authorizations.service.js';

describe('ChargebacksController', () => {
  let controller: ChargebacksController;
  let authorizationsService: AuthorizationsService;

  beforeEach(() => {
    authorizationsService = new AuthorizationsService();
    controller = new ChargebacksController(
      new ChargebacksService(),
      authorizationsService,
    );
  });

  it('creates a chargeback for an existing authorization', () => {
    const authorization = authorizationsService.create({
      cardId: 'card-1',
      merchant: 'Shop',
      amount: 10,
      mcc: '5411',
    });
    const chargeback = controller.create({
      authorizationId: authorization.id,
      reasonCode: '4855',
      description: 'Goods not delivered',
    });
    expect(chargeback.status).toBe('FILED');
  });

  it('throws 404 when the authorization does not exist', () => {
    expect(() =>
      controller.create({
        authorizationId: 'missing',
        reasonCode: '4855',
        description: 'x',
      }),
    ).toThrow(NotFoundException);
  });

  it('rejects an unknown reasonCode', () => {
    const authorization = authorizationsService.create({
      cardId: 'card-1',
      merchant: 'Shop',
      amount: 10,
      mcc: '5411',
    });
    expect(() =>
      controller.create({
        authorizationId: authorization.id,
        reasonCode: '9999',
        description: 'x',
      }),
    ).toThrow(BadRequestException);
  });

  it('rejects an invalid advance status', () => {
    const authorization = authorizationsService.create({
      cardId: 'card-1',
      merchant: 'Shop',
      amount: 10,
      mcc: '5411',
    });
    const chargeback = controller.create({
      authorizationId: authorization.id,
      reasonCode: '4855',
      description: 'x',
    });
    expect(() =>
      controller.advance(chargeback.id, { status: 'BOGUS' }),
    ).toThrow(BadRequestException);
  });
});
