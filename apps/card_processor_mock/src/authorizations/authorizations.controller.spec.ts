import {
  BadRequestException,
  ServiceUnavailableException,
} from '@nestjs/common';
import { AuthorizationsController } from './authorizations.controller.js';
import { AuthorizationsService } from './authorizations.service.js';
import { ProcessorService } from '../processor/processor.service.js';

describe('AuthorizationsController', () => {
  let controller: AuthorizationsController;
  let processorService: ProcessorService;

  beforeEach(() => {
    processorService = new ProcessorService();
    controller = new AuthorizationsController(
      new AuthorizationsService(),
      processorService,
    );
  });

  it('creates an authorization for a valid body', () => {
    const result = controller.create({
      cardId: 'card-1',
      merchant: 'Shop',
      amount: 100,
      mcc: '5411',
    });
    expect(result.approved).toBe(true);
    expect(result.cardId).toBe('card-1');
  });

  it('rejects an invalid mcc', () => {
    expect(() =>
      controller.create({
        cardId: 'card-1',
        merchant: 'Shop',
        amount: 100,
        mcc: '54',
      }),
    ).toThrow(BadRequestException);
  });

  it('rejects a non-positive amount', () => {
    expect(() =>
      controller.create({
        cardId: 'card-1',
        merchant: 'Shop',
        amount: 0,
        mcc: '5411',
      }),
    ).toThrow(BadRequestException);
  });

  it('returns 503 when processor outage is simulated', () => {
    processorService.setOutage(true);
    expect(() =>
      controller.create({
        cardId: 'card-1',
        merchant: 'Shop',
        amount: 100,
        mcc: '5411',
      }),
    ).toThrow(ServiceUnavailableException);
  });

  it('finds authorizations by id after creation', () => {
    const created = controller.create({
      cardId: 'card-1',
      merchant: 'Shop',
      amount: 100,
      mcc: '5411',
    });
    expect(controller.findOne(created.id)).toEqual(created);
  });
});
