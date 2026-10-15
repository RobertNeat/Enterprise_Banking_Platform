import {
  BadRequestException,
  Body,
  Controller,
  Get,
  HttpCode,
  Param,
  Post,
  Query,
  ServiceUnavailableException,
} from '@nestjs/common';
import {
  ChargebacksService,
  type CreateChargebackInput,
} from './chargebacks.service.js';
import type {
  Chargeback,
  ChargebackReasonCode,
  ChargebackStatus,
} from './chargeback.model.js';
import { AuthorizationsService } from '../authorizations/authorizations.service.js';
import { ProcessorService } from '../processor/processor.service.js';

const REASON_CODES: ChargebackReasonCode[] = ['4855', '4837', '4853', '4863'];
const ADVANCE_STATUSES: ChargebackStatus[] = [
  'UNDER_REVIEW',
  'RESOLVED_CUSTOMER',
  'RESOLVED_MERCHANT',
];

@Controller('chargebacks')
export class ChargebacksController {
  constructor(
    private readonly chargebacksService: ChargebacksService,
    private readonly authorizationsService: AuthorizationsService,
    private readonly processorService: ProcessorService,
  ) {}

  @Post()
  create(@Body() body: unknown): Chargeback {
    const input = this.parseCreateInput(body);
    // Validates existence; throws 404 via AuthorizationsService if missing.
    this.authorizationsService.findById(input.authorizationId);
    if (this.processorService.isOutageSimulated()) {
      throw new ServiceUnavailableException('Processor unavailable');
    }
    return this.chargebacksService.create(input);
  }

  @Get()
  findAll(@Query('authorizationId') authorizationId?: string): Chargeback[] {
    return this.chargebacksService.findAll(authorizationId);
  }

  @Get(':id')
  findOne(@Param('id') id: string): Chargeback {
    return this.chargebacksService.findById(id);
  }

  @Post(':id/advance')
  @HttpCode(200)
  advance(@Param('id') id: string, @Body() body: unknown): Chargeback {
    const status = this.parseAdvanceStatus(body);
    if (this.processorService.isOutageSimulated()) {
      throw new ServiceUnavailableException('Processor unavailable');
    }
    return this.chargebacksService.advance(id, status);
  }

  private parseCreateInput(body: unknown): CreateChargebackInput {
    if (typeof body !== 'object' || body === null) {
      throw new BadRequestException('Invalid body');
    }
    const { authorizationId, reasonCode, description } = body as Record<
      string,
      unknown
    >;

    if (
      typeof authorizationId !== 'string' ||
      authorizationId.trim().length === 0
    ) {
      throw new BadRequestException('authorizationId is required');
    }
    if (
      typeof reasonCode !== 'string' ||
      !REASON_CODES.includes(reasonCode as ChargebackReasonCode)
    ) {
      throw new BadRequestException(
        `reasonCode must be one of ${REASON_CODES.join(', ')}`,
      );
    }
    if (typeof description !== 'string' || description.trim().length === 0) {
      throw new BadRequestException('description is required');
    }

    return {
      authorizationId,
      reasonCode: reasonCode as ChargebackReasonCode,
      description,
    };
  }

  private parseAdvanceStatus(body: unknown): ChargebackStatus {
    if (typeof body !== 'object' || body === null) {
      throw new BadRequestException('Invalid body');
    }
    const { status } = body as Record<string, unknown>;
    if (
      typeof status !== 'string' ||
      !ADVANCE_STATUSES.includes(status as ChargebackStatus)
    ) {
      throw new BadRequestException(
        `status must be one of ${ADVANCE_STATUSES.join(', ')}`,
      );
    }
    return status as ChargebackStatus;
  }
}
