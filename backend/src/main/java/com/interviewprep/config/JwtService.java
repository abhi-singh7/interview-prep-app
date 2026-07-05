package com.interviewprep.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Service responsible for generating, parsing, and validating JSON Web Tokens (JWTs) used for authentication.
 *
 * <p>This service creates signed JWTs containing the user's email (as the subject claim) and user ID (as a custom "userId" claim).
 * Tokens are signed using HMAC-SHA256 with a secret key configured via {@link JwtConfig}. The tokens include an expiration time
 * derived from the configuration to ensure they are automatically invalidated after a set duration.</p>
 *
 * <h3>Token Structure:</h3>
 * <ul>
 *   <li><b>Subject (sub):</b> User's email address — used as the primary identifier in JWT claims.</li>
 *   <li><b>userId claim:</b> The JPA entity ID of the authenticated user — allows quick lookup without database queries.</li>
 *   <li><b>issuedAt:</b> Timestamp when the token was created — used for refresh/rotation logic if needed.</li>
 *   <li><b>expiration:</b> Token expiry time derived from {@link JwtConfig#getExpirationMillis()} — tokens past this point are rejected by JJWT's parser.</li>
 * </ul>
 *
 * <h3>Security Notes:</h3>
 * <p>This service uses HMAC-SHA256 symmetric signing. The secret key must be kept confidential and should be at least 256 bits (32 bytes) long.
 * In production, configure a strong random secret via the {@code interviewprep.jwt.secret} property in application.properties.</p>
 *
 * @see JwtConfig
 * @see JwtAuthenticationFilter
 */
@Component
public class JwtService {

    private final JwtConfig jwtConfig;

    /**
     * Constructs the JwtService with JWT configuration.
     *
     * @param jwtConfig The configuration bean containing the signing secret and expiration settings.
     */
    public JwtService(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    /**
     * Generates a new JWT token for the given user email and ID.
     *
     * <p>The generated token includes:</p>
     * <ul>
     *   <li><b>Subject:</b> The user's email address.</li>
     *   <li><b>userId claim:</b> The JPA entity ID of the user.</li>
     *   <li><b>issuedAt:</b> Current timestamp.</li>
     *   <li><b>expiration:</b> Current time plus the configured expiration duration.</li>
     * </ul>
     *
     * @param email The user's email address (becomes the JWT subject claim).
     * @param userId The JPA entity ID of the authenticated user.
     * @return A compact, signed JWT string ready to be set as an HttpOnly cookie value.
     */
    public String generateToken(String email, Long userId) {
        // Derive HMAC-SHA256 signing key from the configured secret
        SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getJwtSecret().getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtConfig.getExpirationMillis()))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts the user ID from a JWT token's "userId" claim.
     *
     * <p>The token is verified and parsed using the configured secret key. If the token is invalid or expired,
     * an exception will be thrown by JJWT — callers should catch this in their validation logic.</p>
     *
     * @param token The JWT token string to parse.
     * @return The Long user ID stored in the "userId" claim.
     * @throws Exception If the token is invalid, expired, or cannot be parsed with the configured key.
     */
    public Long getUserIdFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getJwtSecret().getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("userId", Long.class);
    }

    /**
     * Extracts the email (subject claim) from a JWT token.
     *
     * <p>The token is verified and parsed using the configured secret key. If the token is invalid or expired,
     * an exception will be thrown by JJWT — callers should catch this in their validation logic.</p>
     *
     * @param token The JWT token string to parse.
     * @return The email address stored as the JWT subject claim.
     * @throws Exception If the token is invalid, expired, or cannot be parsed with the configured key.
     */
    public String getEmailFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getJwtSecret().getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Validates whether a JWT token is syntactically correct and signed with the configured secret.
     *
     * <p>This method checks that the token can be successfully parsed and verified without throwing an exception.
     * It does NOT check for additional business-level validity (e.g., whether the user still exists in the database).</p>
     *
     * @param token The JWT token string to validate.
     * @return {@code true} if the token is valid and can be parsed; {@code false} if any exception occurs during verification.
     */
    public boolean isTokenValid(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getJwtSecret().getBytes(StandardCharsets.UTF_8));
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
