package robert_neat.banking_backend.enforcement;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Generates EN-xxxxx ids matching the seed/frontend convention (see
 * db/changelog/seed/009-enforcement.sql).
 */
@Component
public class EnforcementIdGenerator {

  public String next() {
    int digits = ThreadLocalRandom.current().nextInt(1000, 10000);
    return "EN-" + digits;
  }
}
