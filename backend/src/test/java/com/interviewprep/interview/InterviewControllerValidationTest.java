package com.interviewprep.interview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewprep.config.AuthenticationContext;
import com.interviewprep.domain.*;
import com.interviewprep.question.AiGatewayService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class InterviewControllerValidationTest {

    private MockMvc mockMvc;

    private void setup() throws Exception {
        var auth = mock(AuthenticationContext.class);
        when(auth.getCurrentUserId()).thenThrow(new IllegalStateException("No authenticated user"));

        InterviewController controller = new InterviewController(
            mock(InterviewSetupService.class), mock(QuestionGenerationService.class), 
            mock(InterviewResultService.class), auth,
            mock(InterviewSessionRepository.class), mock(AnswerRepository.class),
            mock(AiGatewayService.class), mock(EvaluationRepository.class), mock(QuestionRepository.class));

        mockMvc = standaloneSetup(controller)
                .setControllerAdvice(new com.interviewprep.config.ValidationExceptionHandler())
                .build();
    }

    @Test
    void startInterview_returns_400_when_difficulty_is_null() throws Exception {
        setup();
        InterviewStartRequest request = new InterviewStartRequest();
        request.setDifficulty(null);
        request.setCount(5);
        mockMvc.perform(post("/interview/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startInterview_returns_400_when_count_is_zero() throws Exception {
        setup();
        InterviewStartRequest request = new InterviewStartRequest();
        request.setDifficulty("medium");
        request.setCount(0);
        mockMvc.perform(post("/interview/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startInterview_returns_400_when_count_is_negative() throws Exception {
        setup();
        InterviewStartRequest request = new InterviewStartRequest();
        request.setDifficulty("easy");
        request.setCount(-5);
        mockMvc.perform(post("/interview/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startInterview_validates_request_structure() throws Exception {
        setup();
        String invalidJson = "{invalid json content";
        mockMvc.perform(post("/interview/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getResults_returns_404_when_session_not_found() throws Exception {
        setup();
        mockMvc.perform(get("/interview/results/1")).andExpect(status().isNotFound());
    }
}
