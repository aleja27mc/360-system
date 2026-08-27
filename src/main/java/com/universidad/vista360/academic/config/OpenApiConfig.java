package com.universidad.vista360.academic.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI vista360OpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Vista 360 - Student Academic Service")
                        .version("v1")
                        .description("Reference implementation of the Academic Adapter described in the "
                                + "Vista 360 architecture: returns a student's enrolled courses and current grades."))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
