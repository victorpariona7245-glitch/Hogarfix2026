package com.hogarfix.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI hogarfixOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HogarFix API")
                        .description("API REST para gestión de servicios domésticos (amas de casa, técnicos y admin)")
                        .version("1.0.0"));
    }
}
