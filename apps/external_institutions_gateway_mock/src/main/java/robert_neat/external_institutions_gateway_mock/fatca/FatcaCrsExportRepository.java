package robert_neat.external_institutions_gateway_mock.fatca;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FatcaCrsExportRepository extends JpaRepository<FatcaCrsExport, String> {

    List<FatcaCrsExport> findAllByOrderByGeneratedAtAsc();
}
