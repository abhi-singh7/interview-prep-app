package com.interviewprep.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

/**
 * Servlet filter that exposes the CSRF token as an HttpOnly cookie for cross-origin requests.
 * <p>
 * Spring Security's default CSRF implementation stores the token in a request attribute, which works fine
 * for same-origin requests but is inaccessible to JavaScript running on a different origin (e.g., when the
 * frontend SPA is served from {@code localhost:4200} and the backend API is at {@code localhost:8080}).
 * This filter intercepts CSRF token generation requests and writes the token value into an HttpOnly cookie,
 * enabling cross-origin AJAX calls to include the token in their headers.
 * </p>
 * <h3>Behavior:</h3>
 * <ul>
 *   <li>Only processes requests to {@code /csrf-token}.</li>
 *   <li>For non-same-origin requests, sets an HttpOnly cookie named {@code _csrf} with the token value.</li>
 *   <li>Skips processing for same-origin requests (where the request attribute is sufficient).</li>
 * </ul>
 * 
 * @see CsrfCookieFilter
 */
@Component
public class CsrfTokenCookieGenerator extends OncePerRequestFilter {

    /**
     * Processes each request, setting a CSRF token cookie if this is a cross-origin token generation request.
     * <p>
     * When the frontend requests a new CSRF token via {@code GET /csrf-token}, this filter checks whether
     * the request is cross-origin. If so, it writes the token into an HttpOnly cookie named {@code _csrf}
     * so that subsequent API calls can include it in their headers for Spring Security validation.
     * </p>
     * 
     * @param request     the incoming HTTP servlet request
     * @param response    the HTTP servlet response to write the cookie to
     * @param filterChain the next filter in the chain to delegate to
     * @throws ServletException if a servlet-specific error occurs during processing
     * @throws IOException      if an I/O error occurs while handling the request
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // If this is a CSRF token generation request (GET /csrf-token), set the token in cookie for cross-origin requests.
        String path = request.getRequestURI();
        if ("/csrf-token".equals(path)) {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null && !isSameOriginRequest(request)) {
                jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("_csrf", csrfToken.getToken());
                cookie.setHttpOnly(true);
                cookie.setSecure(false); // Set true in production with HTTPS
                cookie.setPath("/");
                response.addCookie(cookie);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Determines whether the request originates from the same origin as this server.
     * <p>
     * Compares the {@code Origin} header against the server's hostname and port to detect cross-origin requests.
     * If the origin ends with the server hostname, it is considered same-origin.
     * </p>
     * 
     * @param request the HTTP servlet request to check
     * @return {@code true} if the request is from the same origin; {@code false} otherwise
     */
    private boolean isSameOriginRequest(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin == null || origin.isEmpty()) return false;
        String host = request.getServerName();
        int port = request.getServerPort();
        String expectedHost = host + ":" + port;
        // For simplicity, just check the hostname part
        return origin.endsWith(host);
    }

    /**
     * Restricts this filter to only process requests to {@code /csrf-token}.
     * <p>
     * All other requests bypass this filter entirely, avoiding unnecessary processing overhead.
     * </p>
     * 
     * @param request the incoming HTTP servlet request
     * @return {@code true} if the filter should be skipped; {@code false} to process the request
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.equals("/csrf-token");
    }
}
