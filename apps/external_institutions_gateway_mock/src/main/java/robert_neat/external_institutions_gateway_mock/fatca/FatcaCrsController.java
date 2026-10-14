package robert_neat.external_institutions_gateway_mock.fatca;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fatca-crs")
public class FatcaCrsController {

    private final FatcaCrsService fatcaCrsService;

    public FatcaCrsController(FatcaCrsService fatcaCrsService) {
        this.fatcaCrsService = fatcaCrsService;
    }

    @PostMapping("/export")
    public FatcaCrsExport export(@RequestBody FatcaCrsExportRequest request) {
        return fatcaCrsService.export(request);
    }
}
