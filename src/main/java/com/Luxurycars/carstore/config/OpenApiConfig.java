package com.Luxurycars.carstore.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Luxury Cars API")
                        .version("1.0.0")
                        .description("REST API for buying luxury cars. " +
                                "Supports car browsing, filtering, image upload, and order placement.")
                        .contact(new Contact()
                                .name("Ankit")
                                .email("ankit@example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
