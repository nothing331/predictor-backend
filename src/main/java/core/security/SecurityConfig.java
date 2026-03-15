package core.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import core.service.JwtService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired private JwtService jwtService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public routes
                .requestMatchers(HttpMethod.GET,  "/v1/markets").permitAll()
                .requestMatchers(HttpMethod.GET,  "/v1/markets/{marketId}").permitAll()
                .requestMatchers(HttpMethod.GET,  "/v1/stream/events").permitAll()
                // Auth routes (login + refresh are public, rest protected)
                .requestMatchers(HttpMethod.POST, "/v1/auth/google").permitAll()
                .requestMatchers(HttpMethod.POST, "/v1/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.POST, "/v1/auth/logout").permitAll()
                // Everything else requires a valid access token
                .anyRequest().authenticated()
            )
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\"}");
                })
            )
            .addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
