package com.interviewprep.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private JwtService jwtService;

    private String testSecret;

    @BeforeEach
    void setUp() {
        // Use a 256-bit secret for testing (32 bytes)
        testSecret = "a]b@c#d$e%f^g&h*i(j)k_l+m=n{o}p|q\\r/s~t`u!v@w#x$y%z^a&b*c";
        lenient().when(jwtConfig.getJwtSecret()).thenReturn(testSecret);
        lenient().when(jwtConfig.getExpirationMillis()).thenReturn(8L * 60 * 60 * 1000); // 8 hours in milliseconds
    }

    @Test
    void generateToken_returns_valid_jwt_token() {
        String token = jwtService.generateToken("test@example.com", 42L);

        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature
    }

    @Test
    void generateToken_contains_correct_subject() {
        String token = jwtService.generateToken("user@example.com", 1L);

        Claims claims = parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo("user@example.com");
    }

    @Test
    void generateToken_contains_user_id_claim() {
        String token = jwtService.generateToken("test@example.com", 99L);

        Claims claims = parseClaims(token);

        assertThat(claims.get("userId", Long.class)).isEqualTo(99L);
    }

    @Test
    void getUserIdFromToken_returns_correct_user_id() {
        String token = jwtService.generateToken("user@example.com", 42L);

        Long userId = jwtService.getUserIdFromToken(token);

        assertThat(userId).isEqualTo(42L);
    }

    @Test
    void getEmailFromToken_returns_correct_email() {
        String token = jwtService.generateToken("john.doe@example.com", 10L);

        String email = jwtService.getEmailFromToken(token);

        assertThat(email).isEqualTo("john.doe@example.com");
    }

    @Test
    void isTokenValid_returns_true_for_valid_token() {
        String token = jwtService.generateToken("test@example.com", 1L);

        boolean valid = jwtService.isTokenValid(token);

        assertThat(valid).isTrue();
    }

    @Test
    void isTokenValid_returns_false_for_null_token() {
        boolean valid = jwtService.isTokenValid(null);

        assertThat(valid).isFalse();
    }

    @Test
    void isTokenValid_returns_false_for_empty_string_token() {
        boolean valid = jwtService.isTokenValid("");

        assertThat(valid).isFalse();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
