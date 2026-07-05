package com.interviewprep.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration bean for JWT (JSON Web Token) settings, loaded from application properties.
 *
 * <p>This class provides access to the HMAC signing secret and token expiration duration used by {@link JwtService}
 * to generate and validate authentication tokens. The values are injected via Spring's {@code @Value} annotation
 * from the following configuration keys:</p>
 * <ul>
 *   <li>{@code jwt.secret} — The HMAC-SHA256 signing secret (must be at least 32 bytes for HS256).</li>
 *   <li>{@code jwt.expiration-hours} — Token lifetime in hours before automatic expiration.</li>
 * </ul>
 *
 * @see JwtService
 */
@Configuration
public class JwtConfig {

    /** The HMAC-SHA256 signing secret used to sign and verify JWT tokens. Must be at least 32 bytes long for HS256. */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /** Token expiration duration in hours. Tokens are automatically invalidated after this period. */
    @Value("${jwt.expiration-hours}")
    private int expirationHours;

    /**
     * Returns the HMAC signing secret used for JWT token generation and validation.
     *
     * @return The raw secret string configured via {@code jwt.secret}.
     */
    public String getJwtSecret() {
        return jwtSecret;
    }

    /**
     * Returns the token expiration duration in hours.
     *
     * @return The number of hours before a JWT token expires.
     */
    public int getExpirationHours() {
        return expirationHours;
    }

    /**
     * Converts the expiration hours to milliseconds for use in token generation.
     *
     * <p>This method calculates the total milliseconds by multiplying hours × 60 (minutes) × 60 (seconds) × 1000 (milliseconds).</p>
     *
     * @return The expiration duration in milliseconds.
     */
    public long getExpirationMillis() {
        return expirationHours * 60L * 60L * 1000L;
    }
}
