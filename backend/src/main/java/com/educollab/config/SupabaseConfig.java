package com.educollab.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class SupabaseConfig {
    
    @Value("${supabase.url}")
    private String supabaseUrl;
    
    @Value("${supabase.service-key}")
    private String serviceKey;
    
    @Bean
    public WebClient supabaseWebClient() {
        return WebClient.builder()
            .baseUrl(supabaseUrl)
            .defaultHeader("apikey", serviceKey)
            .defaultHeader("Authorization", "Bearer " + serviceKey)
            .defaultHeader("Content-Type", "application/json")
            .build();
    }
    
    public String getSupabaseUrl() {
        return supabaseUrl;
    }
    
    public String getServiceKey() {
        return serviceKey;
    }
}

