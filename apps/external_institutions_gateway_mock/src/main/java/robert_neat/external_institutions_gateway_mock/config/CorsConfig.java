package robert_neat.external_institutions_gateway_mock.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serwis jest wolany bezposrednio z przegladarki (banking_web, inny origin), wiec REST API
 * wymaga wlasnej konfiguracji CORS (strony /ui/** nie wymagaja, bo sa renderowane serwerowo).
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOriginPatterns;

    public CorsConfig(@Value("${gateway.cors.allowed-origin-patterns:http://localhost:*}") String allowedOriginPatterns) {
        this.allowedOriginPatterns = allowedOriginPatterns.split(",");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/enforcement/**")
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods("GET", "POST");
        registry.addMapping("/fatca-crs/**")
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods("GET", "POST");
        registry.addMapping("/regulatory-gateway/**")
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods("GET", "POST");
    }
}
