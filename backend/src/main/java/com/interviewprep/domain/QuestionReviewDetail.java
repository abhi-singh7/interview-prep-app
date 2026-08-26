package com.interviewprep.domain;

import lombok.Data;
import java.util.List;

@Data
public class QuestionReviewDetail {
    private Long questionId;
    private QuestionType questionType;
    private String questionText;
    private String title;
    private String description;
    private String codePrompt;

    // User's submitted answer (null when the question was not answered)
    private String userAnswerText;
    private String language;
    private String submittedCode;

    // Reference/correct answer stored with the question at generation time
    private String referenceAnswer;

    // Evaluation payload, present only when an answer was submitted and evaluated
    private Long answerId;
    private Integer score;
    private Boolean isCorrect;
    private String correctnessExplanation;
    private String timeComplexity;
    private String spaceComplexity;
    private String improvedAnswer;
    private List<String> strengths;
    private List<String> weaknesses;
    private EvaluationStatus status;

    // Resolved topic names for display (from comma-separated categoryId in Question)
    private List<String> topicNames;
}
