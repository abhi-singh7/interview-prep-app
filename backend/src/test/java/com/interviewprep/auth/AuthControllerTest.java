package com.interviewprep.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewprep.config.JwtConfig;
import com.interviewprep.config.JwtService;
import com.interviewprep.domain.AuthResponse;
import com.interviewprep.domain.LoginRequest;
import com.interviewprep.domain.RegisterRequest;
import com.interviewprep.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "jwt.secret=test-secret-key-for-unit-tests-only-at-least-32-bytes-long",
    "jwt.expiration-hours=8"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtConfig jwtConfig;

    @Test
    void register_returns_400_when_email_already_exists() throws Exception {
        // Given: a user already exists with this email
        com.interviewprep.domain.User existingUser = new com.interviewprep.domain.User();
        existingUser.setEmail("existing@example.com");
        existingUser.setName("Existing User");
        existingUser.setPassword(passwordEncoder.encode("password123"));
        userRepository.save(existingUser);

        // When & Then: registration with duplicate email fails
        RegisterRequest request = new RegisterRequest();
        request.setName("New User");
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email already in use"));
    }

    @Test
    void register_returns_ok_with_jwt_cookie() throws Exception {
        // Given: unique email for new user
        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail("newuser@example.com");
        request.setPassword("password123");

        // When & Then: registration succeeds with JWT cookie set
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(cookie().exists("jwt_token"));
    }

    @Test
    void login_returns_400_when_credentials_invalid() throws Exception {
        // Given: user exists with correct email but wrong password
        com.interviewprep.domain.User existingUser = new com.interviewprep.domain.User();
        existingUser.setEmail("user@example.com");
        existingUser.setName("Test User");
        existingUser.setPassword(passwordEncoder.encode("correctpassword"));
        userRepository.save(existingUser);

        // When: login with wrong password
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("wrongpassword");

        // Then: returns bad request
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    @Test
    void login_returns_ok_with_jwt_cookie() throws Exception {
        // Given: user exists with known credentials
        com.interviewprep.domain.User existingUser = new com.interviewprep.domain.User();
        existingUser.setEmail("loginuser@example.com");
        existingUser.setName("Login User");
        existingUser.setPassword(passwordEncoder.encode("password123"));
        userRepository.save(existingUser);

        // When: login with correct credentials
        LoginRequest request = new LoginRequest();
        request.setEmail("loginuser@example.com");
        request.setPassword("password123");

        // Then: returns OK with JWT cookie and user details
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.email").value("loginuser@example.com"))
                .andExpect(jsonPath("$.name").value("Login User"))
                .andExpect(cookie().exists("jwt_token"));
    }

    @Test
    void logout_returns_ok_with_deleted_cookie() throws Exception {
        // When: POST /auth/logout is called
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"))
                .andExpect(cookie().maxAge("jwt_token", 0));
    }

    @Test
    void status_returns_401_when_no_valid_token() throws Exception {
        // When: GET /auth/status without cookie
        mockMvc.perform(get("/auth/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.authenticated").value(false));
    }
}
