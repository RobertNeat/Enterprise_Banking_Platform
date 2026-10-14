import { Module } from '@nestjs/common';
import { ProcessorController } from './processor.controller.js';
import { ProcessorService } from './processor.service.js';

@Module({
  controllers: [ProcessorController],
  providers: [ProcessorService],
  exports: [ProcessorService],
})
export class ProcessorModule {}
