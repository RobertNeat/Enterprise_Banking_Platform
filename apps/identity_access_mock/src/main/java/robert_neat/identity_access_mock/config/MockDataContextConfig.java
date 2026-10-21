package robert_neat.identity_access_mock.config;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Maps identity-access.seed-mock-data (bound from IDENTITY_ACCESS_SEED_MOCK_DATA) to the Liquibase
 * "mock-data" context. Liquibase treats an empty/unset context list as "run everything" including
 * seed changesets, so when seeding is disabled we must set a non-empty, non-matching context
 * rather than leaving it blank. Mirrors governance_mock's MockDataContextConfig.
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
                            beanFactory.resolveEmbeddedValue("${identity-access.seed-mock-data:true}"));
                    liquibase.setContexts(seed ? "mock-data" : "schema-only");
                }
                return bean;
            }
        };
    }
}
