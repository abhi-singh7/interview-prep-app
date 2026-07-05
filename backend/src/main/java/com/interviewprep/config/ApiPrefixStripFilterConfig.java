package com.interviewprep.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;

/**
 * Spring configuration that registers a servlet filter to strip the {@code /api} prefix from incoming request URIs.
 * <p>
 * This filter is part of the application's URL rewriting strategy, allowing the backend to serve both API endpoints
 * (with the {@code /api/} prefix) and SPA static resources (without it). When a request arrives at {@code /api/some-path},
 * the filter rewrites it to {@code /some-path} before passing it to the next filter in the chain. This enables
 * Spring MVC route matching without requiring explicit prefix handling in controllers.
 * </p>
 * <h3>Filter Behavior:</h3>
 * <ul>
 *   <li>If the request URI starts with {@code /api/}, it is rewritten to remove that prefix.</li>
 *   <li>A wrapped {@link HttpServletRequest} is used so downstream components see the rewritten path.</li>
 *   <li>Requests without the {@code /api/} prefix pass through unchanged.</li>
 * </ul>
 * 
 * @see ApiPrefixStripFilterConfig.ApiPrefixStripFilter
 */
@Configuration
public class ApiPrefixStripFilterConfig {

    /**
     * Registers the {@link ApiPrefixStripFilter} as a Spring-managed servlet filter.
     * <p>
     * The filter is mapped to all URL patterns ({@code /*}) and runs with order 10, ensuring it executes early
     * in the filter chain — before other filters like CSRF handling or JWT authentication — so that URI rewriting
     * happens before any path-based logic.
     * </p>
     * 
     * @return a {@link FilterRegistrationBean} configured for the API prefix stripping filter
     */
    @Bean
    public FilterRegistrationBean<ApiPrefixStripFilter> apiPrefixStripFilter() {
        FilterRegistrationBean<ApiPrefixStripFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ApiPrefixStripFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(10);
        return registration;
    }

    /**
     * Servlet filter that strips the {@code /api} prefix from incoming request URIs.
     * <p>
     * When a request arrives with a URI starting in {@code /api/}, this filter creates a wrapped
     * {@link HttpServletRequest} that reports the rewritten path (without the prefix) to downstream components.
     * This allows Spring MVC controllers to match routes without needing to account for the API prefix.
     * </p>
     * <h3>Example:</h3>
     * <ul>
     *   <li>Incoming: {@code GET /api/interview/start} → Rewritten to: {@code GET /interview/start}</li>
     *   <li>Incoming: {@code POST /api/auth/login} → Rewritten to: {@code POST /auth/login}</li>
     * </ul>
     */
    public static class ApiPrefixStripFilter extends OncePerRequestFilter {

        /**
         * Processes each request, stripping the {@code /api/} prefix if present.
         * <p>
         * If the URI starts with {@code /api/}, a wrapped request is created that returns the rewritten path.
         * Otherwise, the original request passes through unchanged.
         * </p>
         * 
         * @param request  the incoming HTTP servlet request (possibly wrapped)
         * @param response the HTTP servlet response to write to
         * @param filterChain the next filter in the chain to delegate to
         * @throws ServletException if a servlet-specific error occurs during processing
         * @throws IOException      if an I/O error occurs while handling the request
         */
        @Override
        protected void doFilterInternal(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            String uri = request.getRequestURI();
            if (uri.startsWith("/api/")) {
                String rewrittenUri = uri.replaceFirst("^/api/", "/");
                HttpServletRequest wrappedRequest = new ApiPrefixStripRequest(request, rewrittenUri);
                filterChain.doFilter(wrappedRequest, response);
                return;
            }
            filterChain.doFilter(request, response);
        }

        /**
         * A {@link HttpServletRequestWrapper} that returns a custom (rewritten) request URI.
         * <p>
         * This wrapper is used to transparently rewrite the request path without modifying the original
         * request object, ensuring downstream components see the correct path for route matching.
         * </p>
         */
        private static class ApiPrefixStripRequest extends HttpServletRequestWrapper {
            private final String requestURI;

            /**
             * Creates a wrapped request with the specified rewritten URI.
             * 
             * @param original   the original HTTP servlet request
             * @param requestURI the new request URI (with {@code /api/} prefix stripped)
             */
            ApiPrefixStripRequest(HttpServletRequest original, String requestURI) {
                super(original);
                this.requestURI = requestURI;
            }

            /**
             * Returns the rewritten request URI.
             * 
             * @return the new request path (e.g., {@code /interview/start} instead of {@code /api/interview/start})
             */
            @Override
            public String getRequestURI() {
                return requestURI;
            }

            /**
             * Always returns {@code null} to indicate that no additional path info is available.
             * <p>
             * Since the URI has been rewritten, any original path info (from servlet mapping) becomes invalid.
             * </p>
             * 
             * @return always {@code null}
             */
            @Override
            public String getPathInfo() {
                return null;
            }
        }
    }
}
