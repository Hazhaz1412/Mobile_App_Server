package com.example.demo.config;

import com.example.demo.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable()) // Disable CORS in Spring Security, handle via @CrossOrigin
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                // Allow static resources
                .requestMatchers("/uploads/**").permitAll()
                
                // Allow OPTIONS requests for CORS
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // Public API endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/users/**").permitAll()
                .requestMatchers("/api/profiles/**").permitAll()
                .requestMatchers("/api/listings/**").permitAll()
                .requestMatchers("/api/conditions/**").permitAll()
                .requestMatchers("/api/categories/**").permitAll()
                
                .anyRequest().authenticated()
            );
        
        // Add JWT token filter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}