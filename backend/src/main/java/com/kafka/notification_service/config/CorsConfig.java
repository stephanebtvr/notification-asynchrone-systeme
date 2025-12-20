package com.kafka.notification_service.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration CORS globale pour autoriser les requêtes cross-origin.
 * 
 * Problème résolu : 
 * Le frontend (http://127.0.0.1:5500 ou http://localhost:4200) 
 * appelle le backend (http://localhost:8080).
 * Sans cette config, le navigateur bloque les requêtes (Same-Origin Policy).
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // Tous les endpoints
                .allowedOriginPatterns("*")  // Toutes les origines en dev
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
        
        System.out.println("✅ CORS configuration loaded - All origins allowed");
    }
}