package com.duoc.cursos.config;

import com.duoc.cursos.security.CognitoRoles;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CorsProperties corsProperties;
    private final String issuerUri;
    private final String jwkSetUri;
    private final String clientId;

    public SecurityConfig(
            CorsProperties corsProperties,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${aws.cognito.client-id}") String clientId
    ) {
        this.corsProperties = corsProperties;
        this.issuerUri = issuerUri;
        this.jwkSetUri = jwkSetUri;
        this.clientId = clientId;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
            com.duoc.cursos.service.SesionUsuarioService sessions,
            @org.springframework.beans.factory.annotation.Qualifier("handlerExceptionResolver")
            org.springframework.web.servlet.HandlerExceptionResolver errors) throws Exception {
        http.addFilterAfter(new com.duoc.cursos.security.UsuarioSincronizadoFilter(sessions, errors),
                org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class)
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/registro",
                                "/api/auth/olvide-contrasena/**",
                                "/api/health",
                                "/actuator/health"
                        ).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                ))
                .formLogin(form -> form.disable())
                .httpBasic(httpBasic -> httpBasic.disable());

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> defaultValidator = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> cognitoValidator = new com.duoc.cursos.security.CognitoAccessTokenValidator(clientId);

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaultValidator, cognitoValidator));
        return decoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::authoritiesFromJwt);
        return converter;
    }

    private Collection<GrantedAuthority> authoritiesFromJwt(Jwt jwt) {
        List<String> groups = jwt.getClaimAsStringList("cognito:groups");
        Set<String> roles = CognitoRoles.normalizeOrDefault(groups == null ? List.of() : groups);
        return roles.stream()
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        if (!corsProperties.isEnabled()) {
            configuration.setAllowedOrigins(List.of());
            configuration.setAllowedMethods(List.of());
            return new UrlBasedCorsConfigurationSource() {
                @Override
                public CorsConfiguration getCorsConfiguration(jakarta.servlet.http.HttpServletRequest request) {
                    return configuration;
                }
            };
        }

        String allowedOrigins = corsProperties.getAllowedOrigins();
        if ("*".equals(allowedOrigins.trim())) {
            configuration.setAllowedOrigins(List.of("*"));
            configuration.setAllowCredentials(false);
        } else {
            configuration.setAllowedOrigins(
                    java.util.Arrays.stream(allowedOrigins.split(","))
                            .map(String::trim)
                            .filter(value -> !value.isEmpty())
                            .toList()
            );
            configuration.setAllowCredentials(corsProperties.isAllowCredentials());
        }

        configuration.setAllowedMethods(
                java.util.Arrays.stream(corsProperties.getAllowedMethods().split(","))
                        .map(String::trim)
                        .toList()
        );
        String allowedHeaders = corsProperties.getAllowedHeaders();
        configuration.setAllowedHeaders("*".equals(allowedHeaders.trim())
                ? List.of("*")
                : java.util.Arrays.stream(allowedHeaders.split(",")).map(String::trim).toList());
        configuration.setExposedHeaders(
                java.util.Arrays.stream(corsProperties.getExposedHeaders().split(","))
                        .map(String::trim)
                        .toList()
        );
        configuration.setMaxAge(corsProperties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
