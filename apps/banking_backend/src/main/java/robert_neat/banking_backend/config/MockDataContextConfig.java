package robert_neat.banking_backend.config;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Maps banking.seed-mock-data (bound from BANKING_SEED_MOCK_DATA) to the Liquibase "mock-data"
 * context. Liquibase treats an empty/unset context list as "run everything" including seed
 * changesets, so when seeding is disabled we must set a non-empty, non-matching context rather
 * than leaving it blank.
 */
@Configuration
public class MockDataContextConfig {

    @Bean
    static BeanPostProcessor mockDataContextPostProcessor(ConfigurableListableBeanFactory beanFactory) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                if (bean instanceof SpringLiquibase liquibase) {
                    boolean seed = Boolean.parseBoolean(
                            beanFactory.resolveEmbeddedValue("${banking.seed-mock-data:false}"));
                    liquibase.setContexts(seed ? "mock-data" : "schema-only");
                }
                return bean;
            }
        };
    }
}
