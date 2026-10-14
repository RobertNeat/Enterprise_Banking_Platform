import { Module } from '@nestjs/common';
import { AppController } from './app.controller.js';
import { AppService } from './app.service.js';
import { AuthorizationsModule } from './authorizations/authorizations.module.js';
import { CardsModule } from './cards/cards.module.js';
import { ChargebacksModule } from './chargebacks/chargebacks.module.js';
import { ProcessorModule } from './processor/processor.module.js';

@Module({
  imports: [
    AuthorizationsModule,
    CardsModule,
    ChargebacksModule,
    ProcessorModule,
  ],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
