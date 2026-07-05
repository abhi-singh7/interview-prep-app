package com.interviewprep.domain;

import lombok.Data;

/**
 * DTO representing the request payload for ending an interview session.
 * <p>
 * This object is sent by the frontend when a user chooses to finish their interview early,
 * triggering the evaluation pipeline to process all submitted answers and generate scores.
 * </p>
 * <p>
 * The {@code sessionId} identifies which active interview session should be marked as completed
 * and have its evaluations generated. Only sessions owned by the authenticated user can be finished.
 * </p>
 */
@Data
public class InterviewFinishRequest {
    /** The unique identifier of the interview session to finish. Must correspond to an ACTIVE session owned by the user. */
    private Long sessionId;
}
