package com.interviewprep.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * Configures Cross-Origin Resource Sharing (CORS) policies for the application.
 * 
 * <p>This configuration allows the Angular frontend (running on a different port during development) to make
 * authenticated requests to the backend API. It permits all HTTP methods and headers from the configured origin,
 * with credentials enabled to allow cookie-based authentication across origins.</p>
 * 
 * <h3>CORS Settings:</h3>
 * <ul>
 *   <li><b>Allowed Origins:</b> Configured via {@code app.frontend.url} property (defaults to {@code http://localhost:4200}).</li>
 *   <li><b>Allowed Methods:</b> GET, POST, PUT, DELETE, OPTIONS.</li>
 *   <li><b>Credentials:</b> Enabled — allows cookies and authentication headers in cross-origin requests.</li>
 *   <li><b>Allowed Headers:</b> All headers permitted (wildcard).</li>
 *   <li><b>Preflight Cache:</b> 3600 seconds (1 hour) — browsers cache preflight responses for this duration.</li>
 * </ul>
 * 
 * @see CorsFilter
 */
@Configuration
public class CorsConfig {

    /** The URL of the Angular frontend application, used as the allowed CORS origin. */
    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    /**
     * Creates and configures a {@link CorsFilter} bean that enforces CORS policies on all incoming requests.
     * 
     * <p>The filter is registered globally via {@code "/**"} to apply to all API endpoints.</p>
     * 
     * @return A configured {@link CorsFilter} instance.
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOrigins(List.of(frontendUrl));
        corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setAllowedHeaders(List.of("*"));
        corsConfiguration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return new CorsFilter(source);
    }
}
