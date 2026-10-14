import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import { App } from 'supertest/types.js';
import { AppModule } from './../src/app.module.js';

describe('AppController (e2e)', () => {
  let app: INestApplication<App>;

  beforeEach(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleFixture.createNestApplication();
    await app.init();
  });

  it('/ (GET)', () => {
    return request(app.getHttpServer())
      .get('/')
      .expect(200)
      .expect('Hello World!');
  });

  it('authorizes, blocks and files a chargeback end to end', async () => {
    const server = app.getHttpServer();

    const authRes = await request(server)
      .post('/authorizations')
      .send({ cardId: 'card-e2e', merchant: 'Shop', amount: 100, mcc: '5411' })
      .expect(201);
    expect(authRes.body.approved).toBe(true);

    await request(server).get(`/authorizations/${authRes.body.id}`).expect(200);
    await request(server)
      .get('/authorizations')
      .query({ cardId: 'card-e2e' })
      .expect(200);

    await request(server).post('/cards/card-e2e/block').expect(200);
    await request(server)
      .get('/cards/card-e2e/block-status')
      .expect(200)
      .expect({ cardId: 'card-e2e', blocked: true });
    await request(server).post('/cards/card-e2e/unblock').expect(200);

    const chargebackRes = await request(server)
      .post('/chargebacks')
      .send({
        authorizationId: authRes.body.id,
        reasonCode: '4855',
        description: 'Goods not delivered',
      })
      .expect(201);
    expect(chargebackRes.body.status).toBe('FILED');

    await request(server)
      .post(`/chargebacks/${chargebackRes.body.id}/advance`)
      .send({ status: 'RESOLVED_CUSTOMER' })
      .expect(409);

    await request(server)
      .post(`/chargebacks/${chargebackRes.body.id}/advance`)
      .send({ status: 'UNDER_REVIEW' })
      .expect(200);
    await request(server)
      .post(`/chargebacks/${chargebackRes.body.id}/advance`)
      .send({ status: 'RESOLVED_CUSTOMER' })
      .expect(200);
  });

  it('404s a chargeback filed against a missing authorization', () => {
    return request(app.getHttpServer())
      .post('/chargebacks')
      .send({
        authorizationId: 'missing',
        reasonCode: '4855',
        description: 'x',
      })
      .expect(404);
  });

  it('503s authorizations while an outage is simulated', async () => {
    const server = app.getHttpServer();

    await request(server)
      .post('/processor/simulate-outage')
      .send({ enabled: true })
      .expect(200)
      .expect({ outageSimulated: true });

    await request(server)
      .post('/authorizations')
      .send({
        cardId: 'card-outage',
        merchant: 'Shop',
        amount: 10,
        mcc: '5411',
      })
      .expect(503);

    await request(server)
      .post('/processor/simulate-outage')
      .send({ enabled: false })
      .expect(200);

    await request(server)
      .post('/authorizations')
      .send({
        cardId: 'card-outage',
        merchant: 'Shop',
        amount: 10,
        mcc: '5411',
      })
      .expect(201);
  });

  afterEach(async () => {
    await app.close();
  });
});
