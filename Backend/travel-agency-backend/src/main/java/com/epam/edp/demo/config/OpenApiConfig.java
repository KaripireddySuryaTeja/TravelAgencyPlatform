package com.epam.edp.demo.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Provide JWT token as: Bearer <token>"
)
public class OpenApiConfig {

    @Value("${app.server.url:http://localhost:8080}")
    private String serverUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        Server productionServer = new Server()
                .url(serverUrl)
                .description("Configured server");

        Server localServer = new Server()
                .url("http://localhost:8080")
                .description("Local development");

        return new OpenAPI()
                .info(new Info()
                        .title("Travel Agency API")
                        .version("v1")
                        .description("REST API for authentication, tours, and bookings.")
                        .contact(new Contact().name("Travel Agency Team")))
                .servers(List.of(productionServer, localServer));
    }
}
