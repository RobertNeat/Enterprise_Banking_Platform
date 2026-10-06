import { buildReportsCsv } from './reporting.csv';
import { RegulatoryReport } from './reporting.model';

describe('buildReportsCsv', () => {
  const reports: RegulatoryReport[] = [
    {
      id: 'RPT-1',
      recipient: 'KNF',
      name: 'Raport; z "cytatem"',
      period: 'Q3 2026',
      dueDate: '2026-10-15',
      status: 'draft',
    },
    {
      id: 'RPT-2',
      recipient: 'NBP',
      name: 'Prosty raport',
      period: '2026',
      dueDate: '2026-10-08',
      status: 'accepted',
    },
  ];

  it('includes the header row and all records when filter is "all"', () => {
    const csv = buildReportsCsv(reports, 'all', (s) => s);
    const lines = csv.split('\n');
    expect(lines).toHaveLength(3);
    expect(lines[0]).toBe('id;recipient;name;period;dueDate;status');
  });

  it('filters rows by recipient', () => {
    const csv = buildReportsCsv(reports, 'NBP', (s) => s);
    const lines = csv.split('\n');
    expect(lines).toHaveLength(2);
    expect(lines[1]).toContain('RPT-2');
  });

  it('quotes fields containing the separator or quotes, doubling inner quotes', () => {
    const csv = buildReportsCsv(reports, 'KNF', (s) => s);
    const lines = csv.split('\n');
    expect(lines[1]).toContain('"Raport; z ""cytatem"""');
  });

  it('applies the status translator', () => {
    const csv = buildReportsCsv(reports, 'all', (s) => s.toUpperCase());
    expect(csv).toContain('DRAFT');
    expect(csv).toContain('ACCEPTED');
  });
});
