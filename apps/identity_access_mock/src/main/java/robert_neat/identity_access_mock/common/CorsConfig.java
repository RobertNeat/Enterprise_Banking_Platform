package robert_neat.identity_access_mock.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** This service is called directly from the browser (banking_web on a different port), hence CORS. */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String allowedOrigin;

    public CorsConfig(@Value("${app.cors.allowed-origin:http://localhost:10500}") String allowedOrigin) {
        this.allowedOrigin = allowedOrigin;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/auth/**")
                .allowedOrigins(allowedOrigin)
                .allowedMethods("GET", "POST")
                .allowedHeaders("*");
        registry.addMapping("/otp/**")
                .allowedOrigins(allowedOrigin)
                .allowedMethods("GET", "POST")
                .allowedHeaders("*");
        registry.addMapping("/devices/**")
                .allowedOrigins(allowedOrigin)
                .allowedMethods("GET", "POST")
                .allowedHeaders("*");
        registry.addMapping("/ebanking-access/**")
                .allowedOrigins(allowedOrigin)
                .allowedMethods("GET", "POST")
                .allowedHeaders("*");
    }
}
