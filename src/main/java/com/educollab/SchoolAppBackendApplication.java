package com.educollab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@SpringBootApplication
@RestController
@CrossOrigin(origins = "*")
public class SchoolAppBackendApplication {
    
    public static void main(String[] args) {
        System.out.println("Starting EduCollab Backend Application...");
        SpringApplication.run(SchoolAppBackendApplication.class, args);
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        System.out.println("Application is ready and listening on port: " + System.getenv("PORT"));
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    @GetMapping("/")
    public String home() {
        System.out.println("Root endpoint accessed");
        return "EduCollab Backend is running!";
    }
    
    @GetMapping("/health")
    public String health() {
        System.out.println("Health endpoint accessed");
        return "OK";
    }
}
