package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class PaymentConfig {
    
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .defaultHeader("User-Agent", "TradeUp-Mobile-App/1.0")
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Content-Type", "application/json");
    }
}
