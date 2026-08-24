package com.interviewprep.domain;

import lombok.Data;
import java.util.List;

@Data
public class EvaluationDetail {
    private Long evaluationId;

    // The answer this evaluation belongs to (used by the retry endpoint)
    private Long answerId;

    private QuestionType questionType;
    private String questionText;
    private Integer score;
    private List<String> strengths;
    private List<String> weaknesses;
    private String improvedAnswer;
    private Boolean isCorrect;
    private String correctnessExplanation;
    private String timeComplexity;
    private String spaceComplexity;
    private EvaluationStatus status;

    // For coding questions - the submitted code
    private String submittedCode;

    // For theory questions - the user's answer text
    private String userAnswerText;

    // Resolved topic names for this evaluation (from comma-separated categoryId in Question)
    private List<String> topicNames;
}
