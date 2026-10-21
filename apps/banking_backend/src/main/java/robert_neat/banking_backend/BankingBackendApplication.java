package robert_neat.banking_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** EnableScheduling backs F8's CAS SLA auto-escalation sweep (cases/CaseSlaEscalationScheduler). */
@SpringBootApplication
@EnableScheduling
public class BankingBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankingBackendApplication.class, args);
    }

}
