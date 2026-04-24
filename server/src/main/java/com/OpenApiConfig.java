package com;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI minesweeperOpenAPI() {
        return new OpenAPI()
                        .info(
                        new Info()
                                .title("Minesweeper API")
                                .description("REST, JWT, partite e punteggi.")
                                .version("0.0.1"));
    }
}
