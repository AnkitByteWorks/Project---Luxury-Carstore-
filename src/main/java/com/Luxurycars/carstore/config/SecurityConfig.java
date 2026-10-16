package com.Luxurycars.carstore.config;

import com.Luxurycars.carstore.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // enables @PreAuthorize on controller methods
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;

    @Autowired
    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          CustomUserDetailsService userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    // ─── Password encoder (BCrypt) ───
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ─── Authentication provider using our UserDetailsService + encoder ───
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    // ─── AuthenticationManager (used by AuthService.login) ───
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ─── MAIN SECURITY RULES ───
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // Disable CSRF (we use JWT, not sessions/cookies)
                .csrf(AbstractHttpConfigurer::disable)

                // Enable CORS with our config below
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Stateless (no HTTP sessions)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Return 401 Unauthorized instead of 403 Forbidden for unauthenticated requests
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new org.springframework.security.web.authentication.HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED)))

                // ─── URL AUTHORIZATION RULES ───
                .authorizeHttpRequests(auth -> auth
                        // Public — auth endpoints
                        .requestMatchers("/api/auth/**").permitAll()

                        // Public — Root & error
                        .requestMatchers("/", "/error").permitAll()

                        // Public — Swagger UI + OpenAPI spec
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // Public — read-only car browsing and view counter
                        .requestMatchers(HttpMethod.GET, "/api/cars/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/cars/*/image").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/cars/*/view").permitAll()

                        // Admin-only — car mutations
                        .requestMatchers(HttpMethod.POST, "/api/cars/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/cars/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/cars/**").hasRole("ADMIN")

                        // VIP Test Drive Booking (Public creation + reference code check + live tracking, admin management)
                        .requestMatchers(HttpMethod.POST, "/api/test-drives").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/test-drives/ref/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/test-drives/*/tracking").permitAll()
                        .requestMatchers("/api/test-drives/**").hasRole("ADMIN")

                        // Analytics & Polyglot Telemetry (MongoDB):
                        // Anonymous interaction/clickstream recording is public for showroom 3D visitors;
                        // Sensitive customer audit logs require ADMIN role.
                        .requestMatchers(HttpMethod.POST, "/api/analytics/interaction").permitAll()
                        .requestMatchers("/api/analytics/customer-audit").hasRole("ADMIN")
                        .requestMatchers("/api/analytics/**").authenticated()

                        // Live Hypercar Auctions:
                        // Browsing active lots, inspecting lot details, and live SSE event streams are public.
                        // Submitting bids requires authenticated collector identity.
                        .requestMatchers(HttpMethod.GET, "/api/auctions/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auctions/*/bid").authenticated()
                        .requestMatchers("/api/auctions/**").hasRole("ADMIN")

                        // Secret Vault & VIP Allocations:
                        // Passcode verification endpoint is public gate; accessing allocations and inquiries require authenticated session.
                        .requestMatchers(HttpMethod.POST, "/api/vault/verify-passcode").permitAll()
                        .requestMatchers("/api/vault/**").authenticated()

                        // Payments:
                        // Public gateway webhook callbacks; payment intent initialization requires authenticated session.
                        .requestMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()
                        .requestMatchers("/api/payments/**").authenticated()

                        // Orders:
                        // Public carrier GPS tracking by tracking link; order management requires authentication.
                        .requestMatchers(HttpMethod.GET, "/api/orders/*/carrier-telemetry").permitAll()
                        .requestMatchers("/api/orders/**").authenticated()

                        // Administrative & Infrastructure Monitoring
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/kafka/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        // Everything else needs authentication
                        .anyRequest().authenticated()
                )

                // Use our provider
                .authenticationProvider(authenticationProvider())

                // Insert our JWT filter before the standard username/password filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ─── CORS config (for frontend on localhost:3000, vercel, etc.) ───
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));  // in prod: list specific origins
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}