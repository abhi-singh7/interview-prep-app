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

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    private boolean isMethod(HttpServletRequest request, String method) {
        return method.equalsIgnoreCase(request.getMethod());
    }

    /** Compare the browser-provided Origin with this request's own scheme://host(:port). */
    private boolean isSameOrigin(HttpServletRequest request, String origin) {
        String hostHeader = request.getHeader("Host");
        if (hostHeader == null || hostHeader.isEmpty()) {
            hostHeader = request.getServerName()
                + (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort());
        }
        String proto = request.getHeader("X-Forwarded-Proto");
        if (proto == null || proto.isEmpty()) {
            proto = request.isSecure() ? "https" : "http";
        }
        return origin.trim().toLowerCase().replaceAll("/+$", "")
                .equals((proto + "://" + hostHeader).toLowerCase());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // FIX (CSRF): SecurityConfig disables Spring CSRF protection (ignoringRequestMatchers("/**")),
        // and the Angular CsrfTokenInterceptor reads the HttpOnly _csrf cookie via document.cookie — always
        // null — so no CSRF token ever reached the server. Enforce a same-site Origin check on
        // state-changing methods here: browsers always send Origin on cross-site requests, so an
        // Origin that doesn't match this request's host is a cross-site submission. We simply
        // refuse to authenticate it, which Spring Security's anyRequest().authenticated() turns
        // into a 403. Non-browser clients (no Origin header) are unaffected and still need a
        // valid JWT cookie. /auth/* is skipped by shouldNotFilter; account-creation CSRF there
        // remains covered by the SameSite=Strict jwt cookie flow.
        boolean crossOrigin = isMethod(request, "POST") || isMethod(request, "PUT")
                || isMethod(request, "PATCH") || isMethod(request, "DELETE");
        if (crossOrigin) {
            String origin = request.getHeader("Origin");
            if (origin != null && !origin.isEmpty() && !isSameOrigin(request, origin)) {
                logger.warn("Cross-origin state-changing request not authenticated (CSRF): path={} origin={}",
                        request.getRequestURI(), origin);
                filterChain.doFilter(request, response);
                return;
            }
        }

        String token = extractToken(request);

        if (token != null && jwtService.isTokenValid(token)) {
            String email = jwtService.getEmailFromToken(token);
            Long userId = jwtService.getUserIdFromToken(token);

            // Set request attributes for authentication context — replaces the anonymous filter in SecurityConfig
            request.setAttribute("currentUserId", userId);
            request.setAttribute("currentEmail", email);

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    email, null, java.util.Collections.emptyList());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }

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

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth/") || path.startsWith("/h2-console");
    }
}
