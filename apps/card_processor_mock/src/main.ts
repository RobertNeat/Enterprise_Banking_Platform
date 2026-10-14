import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module.js';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  app.enableCors({
    origin: (
      process.env.CARD_PROCESSOR_CORS_ALLOWED_ORIGIN ?? 'http://localhost:10500'
    ).split(','),
    methods: ['GET', 'POST'],
  });
  await app.listen(process.env.PORT ?? 10521);
}
await bootstrap();
