package robert_neat.banking_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * RestClient.Builder is NOT auto-configured by spring-boot-starter-web alone in this Boot 4.1
 * module layout (that bean lives behind a web-client autoconfiguration module this project does
 * not pull in) — provide it explicitly so IdentityAccessClient (and MockRestServiceServer in
 * tests, which binds to the builder before .build()) has something to inject.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
