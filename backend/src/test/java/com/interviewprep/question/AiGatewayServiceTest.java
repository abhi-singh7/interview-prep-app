package com.interviewprep.question;

import tools.jackson.databind.json.JsonMapper;

import com.interviewprep.domain.AnswerRepository;
import com.interviewprep.domain.CategoryRepository;
import com.interviewprep.domain.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;

class AiGatewayServiceTest {

    private ChatClient questionChatClientMock;
    private ChatClient evaluationChatClientMock;
    private QuestionRepository questionRepository;
    private AnswerRepository answerRepository;
    private CategoryRepository categoryRepository;
    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        questionChatClientMock = Mockito.mock(ChatClient.class);
        evaluationChatClientMock = Mockito.mock(ChatClient.class);
        questionRepository = Mockito.mock(QuestionRepository.class);
        answerRepository = Mockito.mock(AnswerRepository.class);
        categoryRepository = Mockito.mock(CategoryRepository.class);
        jsonMapper = JsonMapper.builder().build();
    }

    @Test
    void aiGatewayService_instantiates_with_correct_dependencies() {
        var service = new AiGatewayService(
                questionChatClientMock,
                evaluationChatClientMock,
                questionRepository,
                answerRepository,
                categoryRepository,
                jsonMapper);

        // Service instantiated successfully
    }
}
