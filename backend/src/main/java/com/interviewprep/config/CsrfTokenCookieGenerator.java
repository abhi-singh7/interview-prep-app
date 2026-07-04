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

@Component
public class CsrfTokenCookieGenerator extends OncePerRequestFilter {

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

    private boolean isSameOriginRequest(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin == null || origin.isEmpty()) return false;
        String host = request.getServerName();
        int port = request.getServerPort();
        String expectedHost = host + ":" + port;
        // For simplicity, just check the hostname part
        return origin.endsWith(host);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.equals("/csrf-token");
    }
}
