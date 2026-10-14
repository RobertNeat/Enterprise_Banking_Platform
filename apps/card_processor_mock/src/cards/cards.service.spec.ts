import { CardsService } from './cards.service.js';

describe('CardsService', () => {
  beforeEach(() => {
    process.env.CARD_PROCESSOR_SEED_MOCK_DATA = 'false';
  });

  afterEach(() => {
    delete process.env.CARD_PROCESSOR_SEED_MOCK_DATA;
  });

  it('reports unblocked by default', () => {
    const service = new CardsService();
    expect(service.isBlocked('card-1')).toBe(false);
  });

  it('blocks and unblocks a card', () => {
    const service = new CardsService();
    service.block('card-1');
    expect(service.isBlocked('card-1')).toBe(true);
    service.unblock('card-1');
    expect(service.isBlocked('card-1')).toBe(false);
  });

  it('tracks block state independently per card', () => {
    const service = new CardsService();
    service.block('card-1');
    expect(service.isBlocked('card-2')).toBe(false);
  });

  describe('seeding', () => {
    it('seeds CRD-5522 as blocked by default (matches banking_backend payment_card seed)', () => {
      delete process.env.CARD_PROCESSOR_SEED_MOCK_DATA;
      const seeded = new CardsService();
      expect(seeded.isBlocked('CRD-5522')).toBe(true);
    });

    it('seeds nothing when CARD_PROCESSOR_SEED_MOCK_DATA=false', () => {
      const unseeded = new CardsService();
      expect(unseeded.isBlocked('CRD-5522')).toBe(false);
    });
  });
});
