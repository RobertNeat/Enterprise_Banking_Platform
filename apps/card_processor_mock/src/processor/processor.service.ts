import { Injectable } from '@nestjs/common';

// Global in-memory flag simulating processor outage for F4 alt-flow testing
// (banking_backend exercises the ICT incident registration path in governance_mock).
@Injectable()
export class ProcessorService {
  private outageSimulated = false;

  setOutage(enabled: boolean): void {
    this.outageSimulated = enabled;
  }

  isOutageSimulated(): boolean {
    return this.outageSimulated;
  }
}
