package com.equipouno.habitaciones.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${base.url}")
    private String baseUrl;

    @Value("${gateway.url}")
    private String gatewayUrl;

    final String schemeName = "bearerAuth";

    @Bean
    public OpenAPI openApi(){
        return new OpenAPI().info(
                new Info()
                        .title("API de Habitaciones")
                        .version("1.0.0")
                        .description("API para la gestion de habitaciones de un hotel")
                )
                .servers(List.of(
                        new Server()
                                .url(gatewayUrl)
                                .description("API Gateway"),
                        new Server()
                                .url(baseUrl)
                                .description("Servidor local")

                ))
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ))
                .addSecurityItem(new SecurityRequirement().addList(schemeName));
    }
}
