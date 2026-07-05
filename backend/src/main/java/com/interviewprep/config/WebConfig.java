package com.interviewprep.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configures Spring MVC resource handling to serve the compiled Angular frontend from the classpath.
 *
 * <p>This configuration maps all incoming URL patterns ({@code /**}) to static resources located at
 * {@code classpath:/static/browser/}, which contains the built Angular application (HTML, CSS, JS bundles).
 * This enables Spring Boot to serve the SPA directly without requiring a separate web server like Nginx.</p>
 *
 * <h3>Note:</h3>
 * <p>This resource handler works in conjunction with {@link SpaFallbackController}, which handles client-side routing
 * by returning {@code index.html} for non-static paths. The resource handler takes precedence for actual static files
 * (JS, CSS, images), while the fallback controller catches all other routes.</p>
 *
 * @see WebMvcConfigurer
 * @see SpaFallbackController
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** The classpath location of the compiled Angular browser output. */
    private static final String BROWSER_RESOURCES = "classpath:/static/browser/";

    /**
     * Registers a resource handler that serves all URL patterns from the browser resources directory.
     *
     * <p>This mapping ensures that requests for static assets (e.g., {@code /main-XXXX.js}, {@code /styles.css})
     * are served directly from the classpath without hitting any controller.</p>
     *
     * @param registry The resource handler registry to configure.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations(BROWSER_RESOURCES);
    }
}
