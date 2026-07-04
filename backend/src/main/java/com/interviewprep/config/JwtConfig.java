package com.interviewprep.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-hours}")
    private int expirationHours;

    public String getJwtSecret() {
        return jwtSecret;
    }

    public int getExpirationHours() {
        return expirationHours;
    }

    public long getExpirationMillis() {
        return expirationHours * 60L * 60L * 1000L;
    }
}
