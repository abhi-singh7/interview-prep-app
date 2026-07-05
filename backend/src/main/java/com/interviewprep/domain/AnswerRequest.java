package com.interviewprep.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO representing a user's answer submission for an interview question.
 * <p>
 * This object is sent by the frontend when a user submits their answer to a specific question within
 * an active interview session. It contains the question identifier, the answer text content, and
 * optionally the programming language used (for coding questions).
 * </p>
 * <p>
 * Validation constraints ensure:
 * <ul>
 *   <li>{@code questionId} must be provided to identify which question is being answered</li>
 *   <li>{@code answerText} must be non-blank and between 10-50,000 characters for meaningful responses</li>
 *   <li>{@code languageSubmitted} is optional but limited to 50 characters (e.g., "Java", "Python")</li>
 * </ul>
 * </p>
 *
 * @see com.interviewprep.interview.AnswerSubmissionController
 */
@Data
public class AnswerRequest {

    /** The unique identifier of the question being answered. Must reference a valid question in the active session. */
    @NotNull(message = "Question ID is required")
    private Long questionId;

    /**
     * The text content of the user's answer. Must be non-blank and between 10-50,000 characters.
     * <p>
     * For theory questions, this contains the written explanation or response.
     * For coding questions, this may contain code snippets or descriptions of the approach.
     * </p>
     */
    @NotBlank(message = "Answer is required")
    @Size(min = 10, max = 50_000, message = "Answer must be between 10 and 50,000 characters")
    private String answerText;

    /**
     * The programming language used for the answer (e.g., "Java", "Python", "JavaScript").
     * <p>
     * This field is optional but should be provided for coding questions to enable proper evaluation.
     * Language validation is performed conditionally based on question type in the controller layer.
     * </p>
     */
    @Size(max = 50, message = "Language name must not exceed 50 characters")
    private String languageSubmitted;
}
