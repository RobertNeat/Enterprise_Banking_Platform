import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { NestFactory } from '@nestjs/core';
import { NestExpressApplication } from '@nestjs/platform-express';
import { AppModule } from './app.module.js';

const __dirname = dirname(fileURLToPath(import.meta.url));

async function bootstrap() {
  const app = await NestFactory.create<NestExpressApplication>(AppModule);
  app.useStaticAssets(join(__dirname, 'public'));
  app.enableCors({
    origin: (
      process.env.CARD_PROCESSOR_CORS_ALLOWED_ORIGIN ?? 'http://localhost:10500'
    ).split(','),
    methods: ['GET', 'POST'],
  });
  await app.listen(process.env.PORT ?? 10521);
}
await bootstrap();
