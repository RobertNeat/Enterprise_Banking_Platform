import { NotFoundException } from '@nestjs/common';
import { AuthorizationsService } from './authorizations.service.js';

describe('AuthorizationsService', () => {
  let service: AuthorizationsService;

  beforeEach(() => {
    process.env.CARD_PROCESSOR_SEED_MOCK_DATA = 'false';
    service = new AuthorizationsService();
  });

  afterEach(() => {
    delete process.env.CARD_PROCESSOR_SEED_MOCK_DATA;
  });

  it('approves amounts at or below the decline threshold', () => {
    const authorization = service.create({
      cardId: 'card-1',
      merchant: 'Shop',
      amount: 5000,
      mcc: '5411',
    });
    expect(authorization.approved).toBe(true);
    expect(authorization.responseCode).toBe('00');
  });

  it('declines amounts above the decline threshold', () => {
    const authorization = service.create({
      cardId: 'card-1',
      merchant: 'Shop',
      amount: 5000.01,
      mcc: '5411',
    });
    expect(authorization.approved).toBe(false);
    expect(authorization.responseCode).toBe('05');
  });

  it('uses the explicit processor when supplied', () => {
    const authorization = service.create({
      cardId: 'card-1',
      merchant: 'Shop',
      amount: 10,
      mcc: '5411',
      processor: 'Mastercard',
    });
    expect(authorization.processor).toBe('Mastercard');
  });

  it('derives a deterministic processor from cardId when not supplied', () => {
    const first = service.create({
      cardId: 'card-xyz',
      merchant: 'Shop',
      amount: 10,
      mcc: '5411',
    });
    const second = service.create({
      cardId: 'card-xyz',
      merchant: 'Shop2',
      amount: 20,
      mcc: '5412',
    });
    expect(first.processor).toBe(second.processor);
  });

  it('filters authorizations by cardId', () => {
    service.create({
      cardId: 'card-1',
      merchant: 'Shop',
      amount: 10,
      mcc: '5411',
    });
    service.create({
      cardId: 'card-2',
      merchant: 'Shop',
      amount: 10,
      mcc: '5411',
    });
    expect(service.findAll('card-1')).toHaveLength(1);
    expect(service.findAll()).toHaveLength(2);
  });

  it('throws NotFoundException for an unknown id', () => {
    expect(() => service.findById('missing')).toThrow(NotFoundException);
  });

  describe('seeding', () => {
    it('seeds the banking_backend-matching authorizations by default', () => {
      delete process.env.CARD_PROCESSOR_SEED_MOCK_DATA;
      const seeded = new AuthorizationsService();
      expect(seeded.findAll()).toHaveLength(4);
      expect(seeded.findById('449084f7-77b5-4cab-a790-e680763a6572').cardId).toBe(
        'CRD-5530',
      );
    });

    it('seeds nothing when CARD_PROCESSOR_SEED_MOCK_DATA=false', () => {
      const unseeded = new AuthorizationsService();
      expect(unseeded.findAll()).toHaveLength(0);
    });
  });
});
