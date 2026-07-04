package com.interviewprep.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewprep.config.JwtConfig;
import com.interviewprep.config.JwtService;
import com.interviewprep.domain.LoginRequest;
import com.interviewprep.domain.RegisterRequest;
import com.interviewprep.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class AuthControllerTest {

    private MockMvc mockMvc;
    private AuthController controller;

    private void setup(UserRepository repo, PasswordEncoder encoder, JwtService jwt, JwtConfig config) {
        controller = new AuthController(repo, encoder, jwt, config);
        mockMvc = standaloneSetup(controller).build();
    }

    @Test
    void register_returns_400_when_email_already_exists() throws Exception {
        UserRepository repo = org.mockito.Mockito.mock(UserRepository.class);
        when(repo.existsByEmail("existing@example.com")).thenReturn(true);
        setup(repo, org.mockito.Mockito.mock(PasswordEncoder.class), 
              org.mockito.Mockito.mock(JwtService.class), new JwtConfig());

        RegisterRequest req = new RegisterRequest();
        req.setName("New User"); req.setEmail("existing@example.com"); req.setPassword("password123");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email already in use"));
    }

    @Test
    void logout_returns_ok_with_message() throws Exception {
        setup(org.mockito.Mockito.mock(UserRepository.class), 
              org.mockito.Mockito.mock(PasswordEncoder.class),
              org.mockito.Mockito.mock(JwtService.class), new JwtConfig());

        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    void status_returns_401_when_no_token() throws Exception {
        setup(org.mockito.Mockito.mock(UserRepository.class), 
              org.mockito.Mockito.mock(PasswordEncoder.class),
              new JwtService(new JwtConfig()), new JwtConfig());

        mockMvc.perform(get("/auth/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.authenticated").value(false));
    }
}
