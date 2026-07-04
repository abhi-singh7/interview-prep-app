package com.interviewprep.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIntegrationTest {

    @LocalServerPort
    private int port;

    private RestClient client() {
        return RestClient.builder().baseUrl("http://localhost:" + port).build();
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void auth_flow_register_returns_jwt_cookie_and_valid_response() {
        String registerBody = """
            {"name":"Integration Test User","email":"inttest_%d@example.com","password":"Password123!"}
            """.formatted(System.nanoTime());

        ResponseEntity<Map> regResp = client().post().uri("/auth/register")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(registerBody).retrieve()
                .toEntity(Map.class);

        assertThat(regResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(regResp.getBody()).containsKey("email");
        HttpHeaders regHeaders = regResp.getHeaders();
        assertThat(regHeaders.get(HttpHeaders.SET_COOKIE)).isNotNull().isNotEmpty();
    }

    @Test
    void login_with_wrong_password_returns_error_status() {
        String body = """
            {"email":"nonexistent@example.com","password":"WrongPassword"}
            """;

        try {
            client().post().uri("/auth/login")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body).retrieve().toEntity(Map.class);
            assertThat(true).isFalse(); // Should not succeed
        } catch (HttpStatusCodeException e) {
            assertThat(e.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    void status_without_token_returns_403() {
        try {
            client().get().uri("/auth/status")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve().toEntity(Map.class);
            assertThat(true).isFalse(); // Should not succeed
        } catch (HttpStatusCodeException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    void logout_with_valid_session_returns_ok() {
        ResponseEntity<Map> logoutResp = client().post().uri("/auth/logout")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve().toEntity(Map.class);

        assertThat(logoutResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        String setCookieHeader = logoutResp.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeader).startsWith("jwt_token=");
        assertThat(setCookieHeader).contains("Max-Age=0");
    }

    @Test
    void register_with_duplicate_email_returns_error() {
        String email = "dup_%d@example.com".formatted(System.nanoTime());
        String body1 = """
            {"name":"User One","email":"%s","password":"Password123!"}""".formatted(email);

        client().post().uri("/auth/register")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body1).retrieve().toEntity(Map.class);

        // Duplicate email should fail
        try {
            client().post().uri("/auth/register")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body1).retrieve().toEntity(Map.class);
            assertThat(true).isFalse(); // Should not succeed
        } catch (HttpStatusCodeException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Test
    void register_with_blank_fields_returns_400() {
        String body = """
            {"name":"","email":"x@y.com","password":""}
            """;

        try {
            client().post().uri("/auth/register")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body).retrieve().toEntity(Map.class);
            assertThat(true).isFalse(); // Should not succeed
        } catch (HttpStatusCodeException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Test
    void register_with_invalid_email_format_returns_400() {
        String body = """
            {"name":"Bad Email","email":"not-an-email","password":"Password123!"}
            """;

        try {
            client().post().uri("/auth/register")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body).retrieve().toEntity(Map.class);
            assertThat(true).isFalse(); // Should not succeed
        } catch (HttpStatusCodeException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }
}
