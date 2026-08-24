package com.fdic.tip.emailmanager.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TIP Email Manager System API")
                        .version("1.0.0")
                        .description("API documentation for Managing Data Connections, No-Reply Mailbox, and Internal Domain Allowlist.")
                        .contact(new Contact().name("FDIC TIP System Support")));
    }
}