package com.equipouno.gateway.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration corsConfiguration = new CorsConfiguration();
                    corsConfiguration.setAllowedOrigins(List.of("http://localhost:4200"));
                    corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                    corsConfiguration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
                    corsConfiguration.setAllowCredentials(true);
                    return corsConfiguration;
                })).authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(
                                "/api/*/swagger-ui/index.html",
                                "/api/*/swagger-ui/**",
                                "/api/*/v3/api-docs/**"
                        ).permitAll()
                        // Huespedes API security rules
                        .pathMatchers(HttpMethod.GET, "/api/huespedes/**").hasAnyRole("ADMIN", "USER", "ROLE_PANCHITO")
                        .pathMatchers(HttpMethod.POST, "/api/huespedes/**").hasAnyRole("ADMIN", "USER", "ROLE_PANCHITO")
                        .pathMatchers(HttpMethod.PUT, "/api/huespedes/**").hasAnyRole("ADMIN", "USER", "ROLE_PANCHITO")
                        .pathMatchers(HttpMethod.PATCH, "/api/huespedes/**").hasAnyRole("ADMIN", "ROLE_PANCHITO")
                        .pathMatchers(HttpMethod.DELETE, "/api/huespedes/**").hasAnyRole("ADMIN", "ROLE_PANCHITO")
                        // Habitaciones API security rules
                        .pathMatchers(HttpMethod.GET, "/api/habitaciones/**").hasAnyRole("ADMIN", "USER", "ROLE_PANCHITO")
                        .pathMatchers(HttpMethod.POST, "/api/habitaciones/**").hasAnyRole("ADMIN", "ROLE_PANCHITO")
                        .pathMatchers(HttpMethod.PUT, "/api/habitaciones/**").hasAnyRole("ADMIN", "ROLE_PANCHITO")
                        .pathMatchers(HttpMethod.PATCH, "/api/habitaciones/**").hasAnyRole("ADMIN", "ROLE_PANCHITO")
                        .pathMatchers(HttpMethod.DELETE, "/api/habitaciones/**").hasAnyRole("ADMIN", "ROLE_PANCHITO")
                        // Reservas API security rules
                        .pathMatchers(HttpMethod.GET, "/api/reservas/**").hasAnyRole("ADMIN", "USER", "ROLE_PANCHITO")
                        .pathMatchers(HttpMethod.POST, "/api/reservas/**").hasAnyRole("ADMIN", "USER", "ROLE_PANCHITO")
                        .pathMatchers(HttpMethod.PUT, "/api/reservas/**").hasAnyRole("ADMIN", "USER", "ROLE_PANCHITO")
                        .pathMatchers(HttpMethod.PATCH, "/api/reservas/**").hasAnyRole("ADMIN", "USER", "ROLE_PANCHITO")
                        .pathMatchers(HttpMethod.DELETE, "/api/reservas/**").hasAnyRole("ADMIN", "ROLE_PANCHITO")
                        .anyExchange().authenticated()
                ).oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(reactiveJwtAuthenticationConverterAdapter())));

        return http.build();

    }

    @Bean
    ReactiveJwtAuthenticationConverterAdapter reactiveJwtAuthenticationConverterAdapter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
    }

}