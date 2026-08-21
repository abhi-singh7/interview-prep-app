package com.interviewprep.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtRequestInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;

    public JwtRequestInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

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
