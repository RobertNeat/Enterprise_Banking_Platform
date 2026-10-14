import { BadRequestException } from '@nestjs/common';
import { CardsController } from './cards.controller.js';
import { CardsService } from './cards.service.js';

describe('CardsController', () => {
  let controller: CardsController;

  beforeEach(() => {
    controller = new CardsController(new CardsService());
  });

  it('blocks a card', () => {
    expect(controller.block('card-1')).toEqual({
      cardId: 'card-1',
      blocked: true,
    });
    expect(controller.getBlockStatus('card-1')).toEqual({
      cardId: 'card-1',
      blocked: true,
    });
  });

  it('unblocks a card', () => {
    controller.block('card-1');
    expect(controller.unblock('card-1')).toEqual({
      cardId: 'card-1',
      blocked: false,
    });
  });

  it('rejects an empty cardId', () => {
    expect(() => controller.block('   ')).toThrow(BadRequestException);
  });
});
