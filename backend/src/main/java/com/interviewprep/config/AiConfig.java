package com.interviewprep.config;

import tools.jackson.databind.json.JsonMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean("questionChatClient")
    ChatClient questionChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        You are a technical interview question generator. Generate high-quality, varied interview questions across different categories and difficulty levels. Always respond with valid JSON only. Do not include any markdown formatting or text outside the JSON array."""
                )
                .build();
    }

    @Bean("evaluationChatClient")
    ChatClient evaluationChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        You are an AI interviewer evaluating answers. Always evaluate using deterministic results. Provide consistent scoring based on the same criteria. Respond with valid JSON only."""
                )
                .build();
    }

    @Bean
    JsonMapper jsonMapper() {
        return JsonMapper.builder().build();
    }
}
