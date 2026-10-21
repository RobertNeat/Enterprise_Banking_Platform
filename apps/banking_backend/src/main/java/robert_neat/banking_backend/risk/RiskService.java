package robert_neat.banking_backend.risk;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RiskService {

  private final CollateralEntryRepository collateralEntries;

  public RiskService(CollateralEntryRepository collateralEntries) {
    this.collateralEntries = collateralEntries;
  }

  public List<CollateralEntryDto> findAll() {
    return collateralEntries.findAll().stream()
        .map(
            c ->
                new CollateralEntryDto(
                    c.getId(),
                    c.getCustomer().getFullName(),
                    c.getType(),
                    c.getValuation(),
                    c.getLtv(),
                    c.getRating(),
                    c.getStage(),
                    c.getProvision()))
        .toList();
  }
}
