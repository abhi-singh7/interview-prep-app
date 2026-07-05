package com.interviewprep.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

/**
 * Servlet filter that intercepts every HTTP request to validate the JWT authentication token.
 *
 * <p>This filter extends Spring's {@link OncePerRequestFilter} to ensure it runs exactly once per request.
 * It performs the following steps when a valid JWT cookie is present:</p>
 * <ol>
 *   <li><b>Token Extraction:</b> Reads the {@code jwt_token} cookie from the incoming HTTP request.</li>
 *   <li><b>Validation:</b> Delegates to {@link JwtService#isTokenValid(String)} to verify the token's signature and expiration.</li>
 *   <li><b>Context Population:</b> Extracts the user's email and ID from the token, then stores them as request attributes
 *       (accessible via {@link AuthenticationContext}).</li>
 *   <li><b>Security Context:</b> Sets a {@link UsernamePasswordAuthenticationToken} in Spring Security's
 *       {@link SecurityContextHolder}, making the authenticated principal available to downstream filters and controllers.</li>
 * </ol>
 *
 * <h3>Filter Exclusion:</h3>
 * <p>The filter skips processing for URLs under {@code /auth/} (registration, login, logout) and
 * {@code /h2-console} since these endpoints do not require authentication.</p>
 *
 * @see JwtService
 * @see AuthenticationContext
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    /**
     * Constructs the filter with the JWT service for token validation.
     *
     * @param jwtService The service responsible for validating and parsing JWT tokens.
     */
    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Executes the authentication filter logic for each incoming request.
     *
     * <p>This method extracts the JWT token from cookies, validates it using {@link JwtService}, and if valid,
     * populates the Spring Security context with the authenticated user's identity (email and user ID).
     * The extracted credentials are also stored as request attributes for access by {@link AuthenticationContext}.</p>
     *
     * @param request  The incoming HTTP servlet request.
     * @param response The HTTP servlet response.
     * @param filterChain The chain of remaining filters to invoke after this filter completes.
     * @throws ServletException If a servlet-level error occurs during filtering.
     * @throws IOException      If an I/O error occurs during filtering.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Extract JWT token from the jwt_token cookie in the request
        String token = extractToken(request);

        // Validate the token and populate security context if valid
        if (token != null && jwtService.isTokenValid(token)) {
            String email = jwtService.getEmailFromToken(token);
            Long userId = jwtService.getUserIdFromToken(token);

            // Store user identity as request attributes for AuthenticationContext access
            request.setAttribute("currentUserId", userId);
            request.setAttribute("currentEmail", email);

            // Create and set the Spring Security authentication token in the security context.
            // The principal is the email; credentials are null (already validated by JwtService).
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    email, null, java.util.Collections.emptyList());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        // Continue the filter chain regardless of authentication status
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT token value from the {@code jwt_token} cookie in the HTTP request.
     *
     * @param request The HTTP servlet request containing cookies.
     * @return The JWT token string if found, or {@code null} if no matching cookie exists.
     */
    private String extractToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            Optional<Cookie> cookie = Arrays.stream(cookies)
                    .filter(c -> "jwt_token".equals(c.getName()))
                    .findFirst();
            return cookie.map(Cookie::getValue).orElse(null);
        }
        return null;
    }

    /**
     * Determines whether this filter should be skipped for a given request.
     *
     * <p>The filter is bypassed for URLs under {@code /auth/} (public authentication endpoints) and
     * {@code /h2-console} (database console) since these do not require JWT-based authentication.</p>
     *
     * @param request The incoming HTTP servlet request.
     * @return {@code true} if the filter should be skipped; {@code false} to process normally.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth/") || path.startsWith("/h2-console");
    }
}
