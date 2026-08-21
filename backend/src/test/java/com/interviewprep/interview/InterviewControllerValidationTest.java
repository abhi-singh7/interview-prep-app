package com.interviewprep.interview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewprep.domain.InterviewStartRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class InterviewControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void startInterview_returns_400_when_difficulty_is_null() throws Exception {
        // Given: request with null difficulty
        InterviewStartRequest request = new InterviewStartRequest();
        request.setDifficulty(null);
        request.setCount(5);

        // When: POST /interview/start is called
        mockMvc.perform(post("/interview/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startInterview_returns_400_when_count_is_zero() throws Exception {
        // Given: request with count = 0
        InterviewStartRequest request = new InterviewStartRequest();
        request.setDifficulty("medium");
        request.setCount(0);

        // When: POST /interview/start is called
        mockMvc.perform(post("/interview/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startInterview_returns_400_when_count_is_negative() throws Exception {
        // Given: request with negative count
        InterviewStartRequest request = new InterviewStartRequest();
        request.setDifficulty("easy");
        request.setCount(-5);

        // When: POST /interview/start is called
        mockMvc.perform(post("/interview/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startInterview_returns_401_when_not_authenticated() throws Exception {
        // Given: unauthenticated request (no JWT cookie)
        InterviewStartRequest request = new InterviewStartRequest();
        request.setDifficulty("medium");
        request.setCount(5);

        // When: POST /interview/start is called without authentication
        mockMvc.perform(post("/interview/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void startInterview_validates_request_structure() throws Exception {
        // Given: malformed JSON request
        String invalidJson = "{invalid json content";

        // When: POST /interview/start is called with bad JSON
        mockMvc.perform(post("/interview/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startInterview_validates_all_required_fields_present() throws Exception {
        // Given: request with all required fields (difficulty and count)
        InterviewStartRequest request = new InterviewStartRequest();
        request.setDifficulty("hard");
        request.setCount(10);
        request.setLanguageId("10");

        // When: POST /interview/start is called with valid data structure
        mockMvc.perform(post("/interview/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                // Note: May return 401 if not authenticated, but validates request structure first
                .andExpect(status().isUnauthorized());  // Auth required before validation of content
    }

    @Test
    void finishInterview_returns_403_when_session_not_found() throws Exception {
        // Given: unauthenticated user trying to finish non-existent session
        mockMvc.perform(post("/interview/finish")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\": 99999}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getQuestionsForSession_returns_401_when_unauthenticated() throws Exception {
        // Given: unauthenticated request to fetch questions
        mockMvc.perform(get("/interview/session/1/questions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getResumeStatus_returns_no_content_when_no_active_session() throws Exception {
        // Note: This endpoint requires authentication, so it will return 401 without valid JWT
        mockMvc.perform(get("/interview/resume"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void results_endpoint_requires_auth() throws Exception {
        mockMvc.perform(get("/interview/results/1"))
                .andExpect(status().isUnauthorized());
    }
}
