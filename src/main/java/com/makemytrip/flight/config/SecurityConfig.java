package com.makemytrip.flight.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration.
 *
 * For this feature module we open up the flight API and WebSocket endpoints
 * so the demo can be explored without authentication headers.
 *
 * In production:
 *  - Replace permitAll() with .hasRole("USER") or JWT token validation.
 *  - Add CSRF token relay for WebSocket handshake if you re-enable CSRF.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless REST + WebSocket (use HTTPS + origin checks in prod)
            .csrf(AbstractHttpConfigurer::disable)

            .authorizeHttpRequests(auth -> auth
                // WebSocket handshake endpoints
                .requestMatchers("/ws/**").permitAll()

                // Flight tracking REST API
                .requestMatchers("/api/flights/**").permitAll()

                // Static frontend dashboard
                .requestMatchers("/", "/index.html", "/dashboard.html",
                                  "/*.js", "/*.css", "/*.ico").permitAll()

                // Everything else requires authentication
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
