import { TestBed } from '@angular/core/testing';
import { AuditEvent } from './audit-event.model';
import { AuditHistoryDialogComponent } from './audit-history-dialog.component';

const EVENTS: AuditEvent[] = [
  {
    id: 'evt-1',
    actor: 'PRC-10234',
    action: 'LOAN_STAGE_CHANGED',
    entityType: 'LOAN_APPLICATION',
    entityId: 'LN-80203',
    timestamp: '2026-10-10T08:00:00Z',
    details: 'application -> decision',
  },
];

describe('AuditHistoryDialogComponent', () => {
  function createComponent() {
    TestBed.configureTestingModule({ imports: [AuditHistoryDialogComponent] });
    const fixture = TestBed.createComponent(AuditHistoryDialogComponent);
    fixture.componentRef.setInput('dialogId', 'test-history-dialog');
    fixture.componentRef.setInput('visible', true);
    fixture.componentRef.setInput('events', EVENTS);
    fixture.componentRef.setInput('loading', false);
    fixture.detectChanges();
    return fixture;
  }

  it('renders one row per audit event with its action/actor/details', () => {
    const fixture = createComponent();
    const row = fixture.nativeElement.querySelector('#test-history-dialog-row-evt-1');
    expect(row).not.toBeNull();
    expect(row.textContent).toContain('LOAN_STAGE_CHANGED');
    expect(row.textContent).toContain('PRC-10234');
    expect(row.textContent).toContain('application -> decision');
  });

  it('renders the empty-results row when there are no events', () => {
    TestBed.configureTestingModule({ imports: [AuditHistoryDialogComponent] });
    const fixture = TestBed.createComponent(AuditHistoryDialogComponent);
    fixture.componentRef.setInput('dialogId', 'test-history-dialog');
    fixture.componentRef.setInput('visible', true);
    fixture.componentRef.setInput('events', []);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('#test-history-dialog-empty')).not.toBeNull();
  });
});
