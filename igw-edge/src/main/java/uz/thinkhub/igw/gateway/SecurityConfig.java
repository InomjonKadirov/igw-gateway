package uz.thinkhub.igw.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for igw-edge.
 *
 * <p>All routes are protected by JWT bearer-token authentication. The JWT
 * is validated against the Keycloak JWKS at
 * {@code spring.security.oauth2.resourceserver.jwt.jwk-set-uri}. Spring
 * Security's Nimbus-backed {@code NimbusJwtDecoder} caches the JWKS
 * (default 5-minute refresh window).
 *
 * <p>The {@code sub} claim is mapped to the principal name (used by
 * downstream filters in PR #10 for per-user rate limiting and audit).
 * Actuator health/info are public so k8s probes can reach them.
 *
 * <p>Phase 0: no IP allowlist, no CORS, no audience claim check. Those
 * follow in PR #10.
 *
 * <p>The {@code local} profile is a wide-open chain used by the k6Smoke
 * Gradle task to drive the gateway without a Keycloak stand. It is
 * intentionally a separate bean so that production deployments never
 * accidentally activate it.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Profile("!local")
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter)
            throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                )
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    /**
     * Wide-open chain for the k6Smoke task. Permits every request so the
     * byte-diff script can curl the proxied /echo without minting a JWT.
     * Never activate in production — see {@code @Profile("!local")} above.
     */
    @Bean
    @Profile("local")
    public SecurityFilterChain localPermissiveFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    /**
     * Maps the {@code sub} claim to the principal name so downstream code
     * (per-user rate limit, audit filter) can read the user id via
     * {@code authentication.getName()}.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName(JwtClaimNames.SUB);
        return converter;
    }
}

