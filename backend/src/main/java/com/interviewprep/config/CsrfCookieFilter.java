package com.interviewprep.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that propagates the CSRF token from request headers to response cookies.
 *
 * <p>This filter is designed for cross-origin requests where the browser may not automatically include
 * the CSRF cookie. It reads the CSRF token from the {@code X-CSRF-TOKEN} request header and sets it as
 * an HttpOnly cookie in the response, ensuring the frontend can access it for subsequent API calls.</p>
 *
 * <h3>Security Notes:</h3>
 * <ul>
 *   <li>The cookie is set with {@code HttpOnly=true} to prevent JavaScript access (XSS protection).</li>
 *   <li>In production, enable the {@code Secure} flag when using HTTPS.</li>
 * </ul>
 *
 * @see jakarta.servlet.http.Cookie
 */
@Component
public class CsrfCookieFilter extends OncePerRequestFilter {

    /** The HTTP header name containing the CSRF token value. */
    private static final String CSRF_HEADER = "X-CSRF-TOKEN";

    /** The cookie name used to store the CSRF token in the response. */
    private static final String CSRF_COOKIE_NAME = "_csrf";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Read CSRF token from the X-CSRF-TOKEN header for cross-origin requests
        String csrfToken = request.getHeader(CSRF_HEADER);

        if (csrfToken != null && !csrfToken.isEmpty()) {
            // Create an HttpOnly cookie with the CSRF token and add it to the response
            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(CSRF_COOKIE_NAME, csrfToken);
            cookie.setHttpOnly(true);
            cookie.setSecure(false); // Set true in production with HTTPS
            cookie.setPath("/");
            response.addCookie(cookie);
        }

        filterChain.doFilter(request, response);
    }
}
