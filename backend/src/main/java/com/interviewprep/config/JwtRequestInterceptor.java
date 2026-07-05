package com.interviewprep.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Spring MVC handler interceptor that validates JWT tokens on every request and populates the authentication context.
 * 
 * <p>This interceptor runs after the {@link JwtAuthenticationFilter} in the servlet filter chain but before the controller method is invoked.
 * It serves as a secondary validation layer, extracting the JWT from cookies, verifying its validity via {@link JwtService},
 * and storing the user's email and ID as request attributes for access by {@link AuthenticationContext}.</p>
 * 
 * <h3>Interceptor Flow:</h3>
 * <ol>
 *   <li><b>Token Extraction:</b> Reads the {@code jwt_token} cookie from the incoming HTTP request.</li>
 *   <li><b>Validation:</b> Delegates to {@link JwtService#isTokenValid(String)} to verify signature and expiration.</li>
 *   <li><b>Context Population:</b> If valid, extracts email and user ID and stores them as request attributes.</li>
 * </ol>
 * 
 * <h3>Note on Redundancy:</h3>
 * <p>This interceptor performs similar work to {@link JwtAuthenticationFilter}. In a production system, consider consolidating
 * both into a single mechanism to avoid duplicate token parsing. Currently, the filter populates Spring Security's context while
 * this interceptor provides direct access via request attributes.</p>
 * 
 * @see HandlerInterceptor
 * @see JwtService
 */
@Component
public class JwtRequestInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;

    /**
     * Constructs the interceptor with the JWT service for token validation.
     *
     * @param jwtService The service responsible for validating and parsing JWT tokens.
     */
    public JwtRequestInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Validates the JWT token from cookies and populates request attributes with user identity.
     * 
     * <p>This method is called by Spring MVC before the controller handler method executes. If a valid JWT cookie is present,
     * it extracts the email and user ID and stores them as request attributes for downstream access via {@link AuthenticationContext}.</p>
     * 
     * @param request  The incoming HTTP servlet request containing cookies.
     * @param response The HTTP servlet response.
     * @param handler  The controller handler method to be invoked (not modified by this interceptor).
     * @return Always returns {@code true} to allow the request to proceed to the controller, regardless of token validity.
     * @throws Exception If an error occurs during request processing.
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = extractJwtToken(request);
        if (token != null && jwtService.isTokenValid(token)) {
            String email = jwtService.getEmailFromToken(token);
            Long userId = jwtService.getUserIdFromToken(token);

            // Set as request attributes so AuthenticationContext can access them
            request.setAttribute("currentUserId", userId);
            request.setAttribute("currentEmail", email);
        }
        return true;
    }

    /**
     * Extracts the JWT token value from the {@code jwt_token} cookie in the HTTP request.
     * 
     * @param request The HTTP servlet request containing cookies.
     * @return The JWT token string if found, or {@code null} if no matching cookie exists.
     */
    private String extractJwtToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
