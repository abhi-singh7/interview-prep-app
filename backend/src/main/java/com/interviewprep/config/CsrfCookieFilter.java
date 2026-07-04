package com.interviewprep.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Read CSRF token from cookie so it can be set on the response for cross-origin requests.
        String csrfToken = request.getHeader("X-CSRF-TOKEN");

        if (csrfToken != null && !csrfToken.isEmpty()) {
            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("_csrf", csrfToken);
            cookie.setHttpOnly(true);
            cookie.setSecure(false); // Set true in production with HTTPS
            cookie.setPath("/");
            response.addCookie(cookie);
        }

        filterChain.doFilter(request, response);
    }
}
