package robert_neat.external_institutions_gateway_mock.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * RegulatoryGatewayService's async resolve() runs on a delayed-executor thread, not the request
 * thread, so it cannot rely on @Transactional (self-invocation bypasses the AOP proxy) -- it needs
 * a programmatic TransactionTemplate instead. Spring Boot does not auto-configure this bean by
 * default even with spring-boot-starter-data-jpa on the classpath.
 */
@Configuration
public class TransactionTemplateConfig {

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
