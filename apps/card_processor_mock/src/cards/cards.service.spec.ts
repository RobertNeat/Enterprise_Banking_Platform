import { CardsService } from './cards.service.js';

describe('CardsService', () => {
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
});
