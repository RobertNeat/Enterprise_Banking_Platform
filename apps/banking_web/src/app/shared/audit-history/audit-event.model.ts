/**
 * Mirrors banking_backend's GovernanceAuditEventResponse (proxy to governance_mock's
 * GET /audit-events) — backs the "Historia" dialog on loan/case/card/enforcement detail views.
 */
export interface AuditEvent {
  id: string;
  actor: string;
  action: string;
  entityType: string;
  entityId: string;
  /** Pre-formatted by banking_backend as 'yyyy-MM-dd HH:mm' in Europe/Warsaw — render as-is. */
  timestamp: string;
  details: string | null;
}
