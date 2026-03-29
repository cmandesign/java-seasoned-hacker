package com.owaspdemo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OWASP Top 10 (2025) Java Demo")
                        .description("Educational demo — vulnerable and secure endpoints side by side")
                        .version("1.0"));
    }

    @Bean
    public GroupedOpenApi vulnerableApi() {
        return GroupedOpenApi.builder()
                .group("vulnerable")
                .pathsToMatch("/api/v1/vulnerable/**")
                .build();
    }

    @Bean
    public GroupedOpenApi secureApi() {
        return GroupedOpenApi.builder()
                .group("secure")
                .pathsToMatch("/api/v1/secure/**")
                .build();
    }
}
