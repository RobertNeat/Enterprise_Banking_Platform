package robert_neat.external_institutions_gateway_mock.fatca;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA entity for a generated FATCA/CRS XML export. {@code reportId} is the primary key — a repeat
 * export with the same reportId overwrites the previous one (same contract as the old
 * Map.put(reportId, ...) behaviour). {@code xml} is a large hand-built XML string, mapped as plain
 * TEXT (not @Lob) — governance_mock's "payload" column uses the same unbounded VARCHAR/TEXT
 * mapping without @Lob, since @Lob on PostgreSQL maps to an "oid" large object, not a TEXT column,
 * and would fail Hibernate's ddl-auto=validate against a plain TEXT column.
 */
@Entity
@Table(name = "fatca_crs_export", schema = "gateway")
public class FatcaCrsExport {

    @Id
    @Column(name = "report_id", length = 50)
    private String reportId;

    @Column(name = "period", length = 20, nullable = false)
    private String period;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "xml", columnDefinition = "TEXT", nullable = false)
    private String xml;

    protected FatcaCrsExport() {
        // JPA
    }

    public FatcaCrsExport(String reportId, String period, Instant generatedAt, String xml) {
        this.reportId = reportId;
        this.period = period;
        this.generatedAt = generatedAt;
        this.xml = xml;
    }

    // Record-style accessors, kept for existing call sites (tests, UI templates).
    public String reportId() {
        return reportId;
    }

    public String period() {
        return period;
    }

    public Instant generatedAt() {
        return generatedAt;
    }

    public String xml() {
        return xml;
    }

    // JavaBean getters, used by Jackson for JSON serialization.
    public String getReportId() {
        return reportId;
    }

    public String getPeriod() {
        return period;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public String getXml() {
        return xml;
    }
}
