import {
  BadRequestException,
  ServiceUnavailableException,
} from '@nestjs/common';
import { CardsController } from './cards.controller.js';
import { CardsService } from './cards.service.js';
import { ProcessorService } from '../processor/processor.service.js';

describe('CardsController', () => {
  let controller: CardsController;
  let processorService: ProcessorService;

  beforeEach(() => {
    processorService = new ProcessorService();
    controller = new CardsController(new CardsService(), processorService);
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

  it('returns 503 on block when processor outage is simulated', () => {
    processorService.setOutage(true);
    expect(() => controller.block('card-1')).toThrow(
      ServiceUnavailableException,
    );
  });

  it('returns 503 on unblock when processor outage is simulated', () => {
    processorService.setOutage(true);
    expect(() => controller.unblock('card-1')).toThrow(
      ServiceUnavailableException,
    );
  });
});
