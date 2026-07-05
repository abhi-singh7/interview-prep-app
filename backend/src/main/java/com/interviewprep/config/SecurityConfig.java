package com.interviewprep.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for the interview preparation platform.
 *
 * <p>This class configures a stateless, JWT cookie-based authentication mechanism using Spring Security's filter chain.
 * It disables CSRF protection (since the frontend is a single-page application communicating via cookies),
 * permits unauthenticated access to public endpoints (registration, login, logout, static assets), and requires
 * authentication for all other requests.</p>
 *
 * <h3>Security Architecture:</h3>
 * <ul>
 *   <li><b>Password Encoding:</b> Uses BCrypt for hashing user passwords before storage.</li>
 *   <li><b>Session Management:</b> Configured as STATELESS — no server-side sessions are created. Authentication is derived entirely from the JWT cookie on each request.</li>
 *   <li><b>Authentication Filter:</b> {@link JwtAuthenticationFilter} is inserted before Spring's default authentication filter to intercept and validate JWT tokens from cookies.</li>
 *   <li><b>CSRF:</b> Disabled because the application uses HttpOnly cookies for token storage, which are inherently protected against CSRF by the SameSite attribute.</li>
 * </ul>
 *
 * @see JwtAuthenticationFilter
 * @see JwtService
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtService jwtService;

    /**
     * Constructs the SecurityConfig with the JWT service for token validation.
     *
     * @param jwtService The service responsible for generating and validating JWT tokens.
     */
    public SecurityConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Provides a BCrypt password encoder bean used to hash user passwords before storage.
     *
     * <p>BCrypt is chosen for its built-in salt generation and configurable work factor,
     * making it resistant to brute-force attacks.</p>
     *
     * @return A new {@link BCryptPasswordEncoder} instance.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Defines the Spring Security filter chain that governs request authorization and authentication.
     *
     * <p>The filter chain is configured as follows:</p>
     * <ol>
     *   <li><b>CSRF disabled</b> — The SPA frontend uses HttpOnly cookies with SameSite=Strict, which provides CSRF protection.</li>
     *   <li><b>Public endpoints</b> — Registration, login, logout (both /auth and /api/auth prefixes), root path, index.html, favicon, and all static assets are accessible without authentication.</li>
     *   <li><b>All other requests</b> — Require a valid JWT token extracted from cookies by {@link JwtAuthenticationFilter}.</li>
     *   <li><b>Stateless sessions</b> — No server-side HTTP session is created; each request is independently authenticated via the JWT cookie.</li>
     * </ol>
     *
     * @param http The {@link HttpSecurity} builder to configure.
     * @return The built {@link SecurityFilterChain}.
     * @throws Exception If an error occurs during filter chain configuration.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Disable CSRF — the SPA uses HttpOnly cookies with SameSite=Strict for inherent CSRF protection
        http.csrf(csrf -> csrf.disable());

        // Configure request authorization rules
        http.authorizeHttpRequests(auth -> auth
            // Allow unauthenticated access to authentication endpoints (both URL prefixes)
            .requestMatchers("/auth/register", "/auth/login", "/auth/logout",
                            "/api/auth/register", "/api/auth/login", "/api/auth/logout").permitAll()
            // Allow unauthenticated access to SPA entry point and static assets
            .requestMatchers("/", "/index.html", "/favicon.ico").permitAll()
            // Allow unauthenticated access to all static file requests (JS, CSS, fonts, images)
            .requestMatchers(request -> {
                String uri = request.getRequestURI();
                return uri.matches(".*\\.(js|css|ttf|woff|woff2|eot|map|ico|png|svg|json)$");
            }).permitAll()
            // All other requests require authentication via JWT cookie
            .anyRequest().authenticated()
        );

        // Configure stateless session management — no server-side sessions are created
        http.sessionManagement(session -> session.sessionCreationPolicy(
            org.springframework.security.config.http.SessionCreationPolicy.STATELESS));

        // Insert the JWT authentication filter before Spring's default authentication filter.
        // This filter extracts the JWT from cookies, validates it, and populates the AuthenticationContext.
        http.addFilterBefore(new JwtAuthenticationFilter(jwtService),
            org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
