package com.pranav.token_bucket_rate_limiter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI configuration for the Token Bucket Rate Limiter API.
 *
 * <p>Swagger UI is available at: <a href="http://localhost:8080/swagger-ui/index.html">
 * http://localhost:8080/swagger-ui/index.html</a>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tokenBucketOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Token Bucket Rate Limiter API")
                        .version("1.0.0")
                        .description(
                                "Enterprise-grade Token Bucket Rate Limiter Service built with Spring Boot 4. " +
                                "Provides client registration, management, and real-time rate-limit enforcement " +
                                "using the Token Bucket algorithm with pessimistic locking."
                        )
                        .contact(new Contact()
                                .name("Pranav Ganorkar")
                                .email("pranav@example.com")
                                .url("https://github.com/pranavganorkar")
                        )
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")
                        )
                );
    }
}