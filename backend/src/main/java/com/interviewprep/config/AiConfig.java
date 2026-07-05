package com.interviewprep.config;

import tools.jackson.databind.json.JsonMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration class that defines AI-related beans for the interview preparation platform.
 *
 * <p>This class configures two specialized {@link ChatClient} instances used by different parts of the system:
 * one for generating interview questions and another for evaluating user answers. Each client is configured with
 * a distinct system prompt to guide the AI model's behavior appropriately.</p>
 *
 * <h3>Bean Overview:</h3>
 * <ul>
 *   <li>{@code questionChatClient} — Used by {@link com.interviewprep.question.AiGatewayService} and
 *       {@link com.interviewprep.interview.QuestionGenerationService} to generate interview questions.</li>
 *   <li>{@code evaluationChatClient} — Used by {@link com.interviewprep.interview.InterviewResultService}
 *       to evaluate user answers against expected criteria.</li>
 * </ul>
 *
 * @see ChatClient
 * @see ChatModel
 */
@Configuration
public class AiConfig {

    /**
     * Creates a {@link ChatClient} bean specialized for generating interview questions.
     *
     * <p>This client is configured with a system prompt that instructs the AI model to:
     * <ul>
     *   <li>Generate high-quality, varied technical interview questions.</li>
     *   <li>Cover different categories (e.g., Data Structures, System Design) and difficulty levels.</li>
     *   <li>Always respond with valid JSON only — no markdown formatting or extraneous text.</li>
     * </ul>
     *
     * @param chatModel The injected {@link ChatModel} bean providing the underlying AI model (e.g., OpenAI, Anthropic).
     * @return A configured {@link ChatClient} instance named "questionChatClient".
     */
    @Bean("questionChatClient")
    ChatClient questionChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        You are a technical interview question generator. Generate high-quality, varied interview questions across different categories and difficulty levels. Always respond with valid JSON only. Do not include any markdown formatting or text outside the JSON array."""
                )
                .build();
    }

    /**
     * Creates a {@link ChatClient} bean specialized for evaluating user answers during mock interviews.
     *
     * <p>This client is configured with a system prompt that instructs the AI model to:
     * <ul>
     *   <li>Evaluate answers using deterministic, consistent scoring criteria.</li>
     *   <li>Provide fair and reproducible evaluations regardless of input phrasing variations.</li>
     *   <li>Always respond with valid JSON only — no markdown formatting or extraneous text.</li>
     * </ul>
     *
     * @param chatModel The injected {@link ChatModel} bean providing the underlying AI model (e.g., OpenAI, Anthropic).
     * @return A configured {@link ChatClient} instance named "evaluationChatClient".
     */
    @Bean("evaluationChatClient")
    ChatClient evaluationChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        You are an AI interviewer evaluating answers. Always evaluate using deterministic results. Provide consistent scoring based on the same criteria. Respond with valid JSON only."""
                )
                .build();
    }

    /**
     * Creates a {@link JsonMapper} bean for Jackson JSON serialization/deserialization.
     *
     * <p>This mapper is used throughout the application to convert between Java objects and JSON,
     * particularly when parsing AI model responses that are returned as JSON strings.</p>
     *
     * @return A configured {@link JsonMapper} instance with default settings.
     */
    @Bean
    JsonMapper jsonMapper() {
        return JsonMapper.builder().build();
    }
}
