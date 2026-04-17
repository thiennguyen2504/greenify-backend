package com.webdev.greenify.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfiguration {

        private static final String BEARER_SCHEME = "bearerAuth";
        private static final String AUTH_BASE_PATH = "/api/v1/auth";

    @Bean
    public OpenAPI greenifyOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Greenify API")
                        .description("API documentation for Greenify backend services")
                        .version("v1")
                        .contact(new Contact()
                                .name("Greenify Team")
                                .email("support@greenify.local"))
                        .license(new License().name("Proprietary")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste JWT access token only, without 'Bearer ' prefix")));
    }

    @Bean
    public OpenApiCustomizer publicAuthEndpointsCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, pathItem) -> {
                if (path.startsWith(AUTH_BASE_PATH)) {
                    pathItem.readOperations().forEach(operation -> operation.setSecurity(List.of()));
                }
            });
        };
    }
}
