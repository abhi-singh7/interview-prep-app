package com.interviewprep.domain;

import lombok.Data;
import java.util.List;

/**
 * DTO representing the response payload when starting a new interview session.
 * <p>
 * This object is returned by the {@link com.interviewprep.interview.InterviewController} after successfully
 * creating an interview session, containing the session identifier and the list of questions to present
 * to the user during the interview.
 * </p>
 * <p>
 * The frontend uses this response to:
 * <ul>
 *   <li>Navigate to the active interview session using {@code sessionId}</li>
 *   <li>Display the generated questions for the user to answer</li>
 * </ul>
 * </p>
 */
@Data
public class InterviewStartResponse {
    /** The unique identifier of the newly created interview session. Used for subsequent API calls (answer submission, finish). */
    private Long sessionId;

    /** List of questions generated for this interview session based on the user's selected topics and difficulty level. */
    private List<Question> questions;
}
