import { BadRequestException } from '@nestjs/common';
import { ProcessorController } from './processor.controller.js';
import { ProcessorService } from './processor.service.js';

describe('ProcessorController', () => {
  let controller: ProcessorController;
  let service: ProcessorService;

  beforeEach(() => {
    service = new ProcessorService();
    controller = new ProcessorController(service);
  });

  it('enables the outage simulation', () => {
    expect(controller.simulateOutage({ enabled: true })).toEqual({
      outageSimulated: true,
    });
    expect(controller.getStatus()).toEqual({ outageSimulated: true });
  });

  it('rejects a non-boolean enabled flag', () => {
    expect(() => controller.simulateOutage({ enabled: 'yes' })).toThrow(
      BadRequestException,
    );
  });

  it('rejects a missing enabled field', () => {
    expect(() => controller.simulateOutage({})).toThrow(BadRequestException);
  });
});
