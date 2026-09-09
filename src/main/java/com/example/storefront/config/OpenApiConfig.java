package com.example.storefront.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Describes the API for the Swagger UI at {@code /swagger} and wires a "bearer"
 * auth button so protected endpoints can be exercised from the browser.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearer-jwt";

    @Bean
    public OpenAPI storefrontOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Storefront API")
                        .version("0.1.0")
                        .description("Catalog and orders, with order fulfilment handled asynchronously over Kafka."))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
