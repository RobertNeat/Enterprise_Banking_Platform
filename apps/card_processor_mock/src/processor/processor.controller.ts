import {
  BadRequestException,
  Body,
  Controller,
  Get,
  HttpCode,
  Post,
} from '@nestjs/common';
import { ProcessorService } from './processor.service.js';

@Controller('processor')
export class ProcessorController {
  constructor(private readonly processorService: ProcessorService) {}

  @Post('simulate-outage')
  @HttpCode(200)
  simulateOutage(@Body() body: unknown): { outageSimulated: boolean } {
    if (typeof body !== 'object' || body === null || !('enabled' in body)) {
      throw new BadRequestException('enabled is required');
    }
    const { enabled } = body as { enabled: unknown };
    if (typeof enabled !== 'boolean') {
      throw new BadRequestException('enabled must be a boolean');
    }
    this.processorService.setOutage(enabled);
    return { outageSimulated: enabled };
  }

  @Get('status')
  getStatus(): { outageSimulated: boolean } {
    return { outageSimulated: this.processorService.isOutageSimulated() };
  }
}
