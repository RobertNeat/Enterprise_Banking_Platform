import { ProcessorService } from './processor.service.js';

describe('ProcessorService', () => {
  it('defaults to no outage', () => {
    const service = new ProcessorService();
    expect(service.isOutageSimulated()).toBe(false);
  });

  it('toggles the outage flag', () => {
    const service = new ProcessorService();
    service.setOutage(true);
    expect(service.isOutageSimulated()).toBe(true);
    service.setOutage(false);
    expect(service.isOutageSimulated()).toBe(false);
  });
});
