package com.interviewprep.domain;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class InterviewSessionDetailResponse {
    private Long sessionId;
    private String status;
    private String languageId;
    private String languageName;
    private List<String> topicNames;
    private String categoryId;
    private String difficulty;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Double overallScore;
    private Integer totalEarned;
    private List<SessionQuestionResponse> questions;

    @Data
    public static class SessionQuestionResponse {
        private Long questionId;
        private String type;
        private String title;
        private String questionText;
        private String description;
        private String codePrompt;
        private String answerStatus;
        private SessionAnswerResponse answer;
    }

    @Data
    public static class SessionAnswerResponse {
        private Long answerId;
        private String answerText;
        private String languageSubmitted;
        private SessionEvaluationResponse evaluation;
    }

    @Data
    public static class SessionEvaluationResponse {
        private Long evaluationId;
        private Integer score;
        private String status;
        private List<String> strengths;
        private List<String> weaknesses;
        private String improvedAnswer;
        private Boolean isCorrect;
        private String correctnessExplanation;
        private String timeComplexity;
        private String spaceComplexity;
    }
}
