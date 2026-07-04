package com.interviewprep.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AnswerRequest {
    
    @NotNull(message = "Question ID is required")
    private Long questionId;
    
    /**
     * User's answer text. Must be non-blank for theory questions or contain code for coding questions.
     * Minimum 10 characters to ensure meaningful answers, maximum 50,000 for long responses.
     */
    @NotBlank(message = "Answer is required")
    @Size(min = 10, max = 50_000, message = "Answer must be between 10 and 50,000 characters")
    private String answerText;

    /**
     * Programming language used for the answer (e.g., Java, Python, JavaScript).
     * Not validated here — validation is conditional based on question type in AnswerSubmissionController.
     */
    @Size(max = 50, message = "Language name must not exceed 50 characters")
    private String languageSubmitted;
}
