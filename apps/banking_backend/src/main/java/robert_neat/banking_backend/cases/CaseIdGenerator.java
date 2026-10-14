package robert_neat.banking_backend.cases;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/** Generates CS-xxxxx ids matching the seed/frontend convention (see db/changelog/seed/006-cases.sql). */
@Component
public class CaseIdGenerator {

    public String next() {
        int digits = ThreadLocalRandom.current().nextInt(10000, 100000);
        return "CS-" + digits;
    }
}
