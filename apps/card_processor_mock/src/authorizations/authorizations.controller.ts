import {
  BadRequestException,
  Body,
  Controller,
  Get,
  Param,
  Post,
  Query,
  ServiceUnavailableException,
} from '@nestjs/common';
import {
  AuthorizationsService,
  type CreateAuthorizationInput,
} from './authorizations.service.js';
import type { CardAuthorization, CardNetwork } from './authorization.model.js';
import { ProcessorService } from '../processor/processor.service.js';

const MCC_PATTERN = /^\d{4}$/;
const NETWORKS: CardNetwork[] = ['Visa', 'Mastercard'];

@Controller('authorizations')
export class AuthorizationsController {
  constructor(
    private readonly authorizationsService: AuthorizationsService,
    private readonly processorService: ProcessorService,
  ) {}

  @Post()
  create(@Body() body: unknown): CardAuthorization {
    if (this.processorService.isOutageSimulated()) {
      throw new ServiceUnavailableException('Processor unavailable');
    }
    const input = this.parseCreateInput(body);
    return this.authorizationsService.create(input);
  }

  @Get()
  findAll(@Query('cardId') cardId?: string): CardAuthorization[] {
    return this.authorizationsService.findAll(cardId);
  }

  @Get(':id')
  findOne(@Param('id') id: string): CardAuthorization {
    return this.authorizationsService.findById(id);
  }

  private parseCreateInput(body: unknown): CreateAuthorizationInput {
    if (typeof body !== 'object' || body === null) {
      throw new BadRequestException('Invalid body');
    }
    const { cardId, merchant, amount, mcc, processor } = body as Record<
      string,
      unknown
    >;

    if (typeof cardId !== 'string' || cardId.trim().length === 0) {
      throw new BadRequestException('cardId is required');
    }
    if (typeof merchant !== 'string' || merchant.trim().length === 0) {
      throw new BadRequestException('merchant is required');
    }
    if (typeof amount !== 'number' || !Number.isFinite(amount) || amount <= 0) {
      throw new BadRequestException('amount must be a positive number');
    }
    if (typeof mcc !== 'string' || !MCC_PATTERN.test(mcc)) {
      throw new BadRequestException('mcc must be a 4-digit string');
    }
    if (
      processor !== undefined &&
      !NETWORKS.includes(processor as CardNetwork)
    ) {
      throw new BadRequestException('processor must be Visa or Mastercard');
    }

    return {
      cardId,
      merchant,
      amount,
      mcc,
      processor: processor as CardNetwork | undefined,
    };
  }
}
