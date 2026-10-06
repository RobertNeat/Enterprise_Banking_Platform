import { RegulatoryReport } from './reporting.model';

/** Escapes a CSV field: quotes it when it contains the separator, a quote or a newline. */
function escapeCsvField(value: string): string {
  if (/[;"\n]/.test(value)) {
    return `"${value.replace(/"/g, '""')}"`;
  }
  return value;
}

/**
 * Builds a `;`-separated CSV (PL Excel convention) from regulatory reports,
 * optionally filtered by recipient. `translateStatus` resolves a status to
 * its current-language label; pass the identity function to keep raw values.
 */
export function buildReportsCsv(
  reports: RegulatoryReport[],
  recipientFilter: string,
  translateStatus: (status: RegulatoryReport['status']) => string,
): string {
  const header = ['id', 'recipient', 'name', 'period', 'dueDate', 'status'];
  const rows = reports
    .filter((r) => recipientFilter === 'all' || recipientFilter === r.recipient)
    .map((r) => [r.id, r.recipient, r.name, r.period, r.dueDate, translateStatus(r.status)]);

  return [header, ...rows].map((row) => row.map(escapeCsvField).join(';')).join('\n');
}
