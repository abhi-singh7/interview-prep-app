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

@Configuration
public class ApiPrefixStripFilterConfig {

    @Bean
    public FilterRegistrationBean<ApiPrefixStripFilter> apiPrefixStripFilter() {
        FilterRegistrationBean<ApiPrefixStripFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ApiPrefixStripFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(10);
        return registration;
    }

    public static class ApiPrefixStripFilter extends OncePerRequestFilter {

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

        private static class ApiPrefixStripRequest extends HttpServletRequestWrapper {
            private final String requestURI;

            ApiPrefixStripRequest(HttpServletRequest original, String requestURI) {
                super(original);
                this.requestURI = requestURI;
            }

            @Override
            public String getRequestURI() {
                return requestURI;
            }

            @Override
            public String getPathInfo() {
                return null;
            }
        }
    }
}
