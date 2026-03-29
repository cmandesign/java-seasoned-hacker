package com.owaspdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Vulnerable endpoints: wide open, no CSRF, no auth required.
     * This simulates a misconfigured application.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain vulnerableFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/v1/vulnerable/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));
        return http.build();
    }

    /**
     * Secure endpoints: proper auth, CSRF for browser clients, strict headers.
     * For demo simplicity we use HTTP Basic — in production you'd use OAuth2/JWT.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain secureFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/v1/secure/**")
            .csrf(AbstractHttpConfigurer::disable) // disabled for REST API demo; enable for browser forms
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .httpBasic(basic -> {});
        return http.build();
    }

    /**
     * Default chain: Actuator, H2 console, etc.
     */
    @Bean
    @Order(3)
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").authenticated()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .anyRequest().permitAll())
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
            .httpBasic(basic -> {});
        return http.build();
    }
}
