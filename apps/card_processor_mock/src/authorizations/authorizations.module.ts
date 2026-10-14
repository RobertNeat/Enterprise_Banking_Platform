import { Module } from '@nestjs/common';
import { AuthorizationsController } from './authorizations.controller.js';
import { AuthorizationsService } from './authorizations.service.js';
import { ProcessorModule } from '../processor/processor.module.js';

@Module({
  imports: [ProcessorModule],
  controllers: [AuthorizationsController],
  providers: [AuthorizationsService],
  exports: [AuthorizationsService],
})
export class AuthorizationsModule {}
