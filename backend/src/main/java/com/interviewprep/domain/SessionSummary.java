package com.interviewprep.domain;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO representing a brief summary of an interview session for display in analytics and history views.
 * <p>
 * This lightweight object is used by the {@code AnalyticsService} to present a list of past sessions
 * without loading full evaluation details. It provides just enough information for the frontend
 * to render a session history table or chart (session ID, date, score, question counts).
 * </p>
 * 
 * @see AnalyticsResponse
 * @see InterviewSession
 */
@Data
public class SessionSummary {

    /** The unique identifier of this interview session. */
    private Long sessionId;

    /** Timestamp when this session ended (completed or abandoned). */
    private LocalDateTime endedAt;

    /** Human-readable name of the category associated with this session's questions. */
    private String categoryName;

    /** Difficulty level of this session (e.g., "easy", "medium", "hard"). */
    private String difficulty;

    /** Average score achieved in this session (0-100 scale). */
    private Integer score;

    /** Total number of questions generated for this session. */
    private Integer questionCount;

    /** Number of questions the user actually submitted answers for. */
    private Integer answeredCount;
}
