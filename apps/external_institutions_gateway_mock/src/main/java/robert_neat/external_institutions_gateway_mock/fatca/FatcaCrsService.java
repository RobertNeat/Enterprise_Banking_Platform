package robert_neat.external_institutions_gateway_mock.fatca;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * XML wygenerowany recznie (String-builder), zgodnie z zasada projektu: brak JAXB jako nowej zaleznosci.
 * Docelowy XSD wymiany FATCA/CRS jest poza zakresem tej iteracji (mock).
 */
@Service
public class FatcaCrsService {

    private final Map<String, FatcaCrsExport> exports = new ConcurrentHashMap<>();

    public FatcaCrsExport export(FatcaCrsExportRequest request) {
        if (isBlank(request.reportId()) || isBlank(request.period()) || isBlank(request.recipient())
                || request.records() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "reportId, period, recipient i records sa wymagane");
        }
        if (!"FATCA/CRS".equals(request.recipient())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "recipient musi byc \"FATCA/CRS\"");
        }

        String xml = buildXml(request);
        FatcaCrsExport created = new FatcaCrsExport(request.reportId(), request.period(), Instant.now(), xml);
        exports.put(request.reportId(), created);
        return created;
    }

    public List<FatcaCrsExport> list() {
        return exports.values().stream()
                .sorted(Comparator.comparing(FatcaCrsExport::generatedAt))
                .toList();
    }

    private String buildXml(FatcaCrsExportRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<FatcaCrsReport reportId=\"").append(escape(request.reportId())).append("\">\n");
        sb.append("  <Period>").append(escape(request.period())).append("</Period>\n");
        sb.append("  <Records>\n");
        for (FatcaCrsRecord record : request.records()) {
            sb.append("    <Record>\n");
            sb.append("      <Customer>").append(escape(record.customer())).append("</Customer>\n");
            sb.append("      <TaxId>").append(escape(record.taxId())).append("</TaxId>\n");
            sb.append("      <Balance>").append(record.balance()).append("</Balance>\n");
            sb.append("    </Record>\n");
        }
        sb.append("  </Records>\n");
        sb.append("</FatcaCrsReport>\n");
        return sb.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
