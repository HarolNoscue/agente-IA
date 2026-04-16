package com.chatbot.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebFluxConfigurer corsConfigurer() {
        return new WebFluxConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/chat")
                        .allowedOrigins(
                                "https://tu-org.lightning.force.com",
                                "http://localhost:8080" // para pruebas
                        )
                        .allowedMethods("POST")
                        .allowedHeaders("*");
            }
        };
    }
}
