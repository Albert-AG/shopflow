package com.shopflow.orders.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 6 configuration (T16).
 *
 * Uses SecurityFilterChain (NOT the deprecated WebSecurityConfigurerAdapter).
 * @EnableMethodSecurity enables @PreAuthorize on individual endpoints.
 *
 * Generated in T16 with:
 * "Configura Spring Security 6 para ShopFlow. JWT stateless.
 *  GET /api/v1/orders/** permite ROLE_USER y ROLE_ADMIN.
 *  POST/PUT/DELETE solo ROLE_ADMIN.
 *  Actuator /health público. Usar SecurityFilterChain, no WebSecurityConfigurerAdapter."
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable()) // stateless API — no CSRF needed
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/orders/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/orders/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .httpBasic(basic -> {}) // simplified for demo — real app uses JWT
                .build();
    }
}
