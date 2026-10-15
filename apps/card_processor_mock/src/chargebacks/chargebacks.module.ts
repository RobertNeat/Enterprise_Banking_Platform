import { Module } from '@nestjs/common';
import { ChargebacksController } from './chargebacks.controller.js';
import { ChargebacksService } from './chargebacks.service.js';
import { AuthorizationsModule } from '../authorizations/authorizations.module.js';
import { ProcessorModule } from '../processor/processor.module.js';

@Module({
  imports: [AuthorizationsModule, ProcessorModule],
  controllers: [ChargebacksController],
  providers: [ChargebacksService],
})
export class ChargebacksModule {}
