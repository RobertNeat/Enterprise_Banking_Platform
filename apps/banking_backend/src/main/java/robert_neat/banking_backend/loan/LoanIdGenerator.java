package robert_neat.banking_backend.loan;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Generates LN-xxxxx ids matching the seed/frontend convention (see
 * db/changelog/seed/002-loan.sql).
 */
@Component
public class LoanIdGenerator {

  public String next() {
    int digits = ThreadLocalRandom.current().nextInt(10000, 100000);
    return "LN-" + digits;
  }
}
