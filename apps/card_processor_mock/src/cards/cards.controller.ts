import {
  BadRequestException,
  Controller,
  Get,
  HttpCode,
  Param,
  Post,
} from '@nestjs/common';
import { CardsService } from './cards.service.js';

interface BlockStatus {
  cardId: string;
  blocked: boolean;
}

@Controller('cards')
export class CardsController {
  constructor(private readonly cardsService: CardsService) {}

  @Post(':cardId/block')
  @HttpCode(200)
  block(@Param('cardId') cardId: string): BlockStatus {
    this.validateCardId(cardId);
    this.cardsService.block(cardId);
    return { cardId, blocked: true };
  }

  @Post(':cardId/unblock')
  @HttpCode(200)
  unblock(@Param('cardId') cardId: string): BlockStatus {
    this.validateCardId(cardId);
    this.cardsService.unblock(cardId);
    return { cardId, blocked: false };
  }

  @Get(':cardId/block-status')
  getBlockStatus(@Param('cardId') cardId: string): BlockStatus {
    this.validateCardId(cardId);
    return { cardId, blocked: this.cardsService.isBlocked(cardId) };
  }

  private validateCardId(cardId: string): void {
    if (!cardId || cardId.trim().length === 0) {
      throw new BadRequestException('cardId is required');
    }
  }
}
