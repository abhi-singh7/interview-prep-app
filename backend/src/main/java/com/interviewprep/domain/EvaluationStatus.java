package com.interviewprep.domain;

/**
 * Enum representing the status of an AI-generated evaluation within an interview session.
 * <p>
 * Evaluations track whether the AI successfully processed and scored a user's answer.
 * The {@link #SUCCESS} status indicates a completed evaluation with valid scores and feedback,
 * while {@link #FAILED} indicates that the evaluation could not be generated (e.g., due to
 * AI service errors or invalid input).
 * </p>
 */
public enum EvaluationStatus {
    /** Evaluation was successfully generated with scores and feedback. */
    SUCCESS,

    /** Evaluation failed to generate - may indicate AI service issues or invalid answer data. */
    FAILED
}
