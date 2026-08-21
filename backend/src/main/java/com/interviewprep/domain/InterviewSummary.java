package com.interviewprep.domain;

/**
 * Summary DTO for displaying interview session information in resume status.
 * 
 * <p>This record provides a typed representation of an interview session's key attributes,
 * replacing the previous raw ObjectNode usage in the /interview/resume endpoint.</p>
 * 
 * <p>Fields include:</p>
 * <ul>
 *   <li>{@code sessionId} - Unique identifier for the interview session</li>
 *   <li>{@code categoryId} - Language/category ID associated with the interview</li>
 *   <li>{@code difficulty} - Interview difficulty level (e.g., "easy", "medium", "hard")</li>
 *   <li>{@code answeredQuestions} - Number of questions already answered by the user</li>
 * </ul>
 */
public record InterviewSummary(
        Long sessionId,
        String categoryId,
        String difficulty,
        int answeredQuestions
) {

    /**
     * Creates an InterviewSummary from an InterviewSession entity.
     * 
     * @param session the source InterviewSession to extract data from
     * @param answeredCount number of answers submitted for this session
     * @return a new InterviewSummary instance with values from the session and answer count
     */
    public static InterviewSummary from(InterviewSession session, int answeredCount) {
        return new InterviewSummary(
                session.getId(),
                session.getCategoryId(),
                session.getDifficulty(),
                answeredCount
        );
    }

    /**
     * Converts this summary to a Map for compatibility with existing response handling.
     * 
     * @return a Map representation of this summary DTO
     */
    public java.util.Map<String, Object> toMap() {
        return new java.util.HashMap<>() {{
            put("sessionId", sessionId);
            put("categoryId", categoryId != null ? categoryId : "N/A");
            put("difficulty", difficulty != null ? difficulty : "N/A");
            put("answeredQuestions", answeredQuestions);
        }};
    }
}
