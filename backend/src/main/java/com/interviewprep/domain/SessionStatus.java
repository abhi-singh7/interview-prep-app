package com.interviewprep.domain;

/**
 * Enum representing the lifecycle status of an interview session.
 * 
 * <p>This enum tracks the state transitions of a user's mock interview practice session from creation to completion or abandonment.</p>
 * 
 * <h3>Status Transitions:</h3>
 * <ul>
 *   <li>{@link #ACTIVE} — Session is in progress, questions are being generated and answered.</li>
 *   <li>{@link #COMPLETED} — User has finished the session normally by submitting all answers.</li>
 *   <li>{@link #ABANDONED} — Session exceeded its timeout period without completion (set by {@code SessionTimeoutCleanupTask}).</li>
 * </ul>
 * 
 * @see InterviewSession
 */
public enum SessionStatus {
    /** The interview session is currently active and in progress. */
    ACTIVE,
    
    /** The user has completed the interview session normally. */
    COMPLETED,
    
    /** The session was abandoned due to exceeding the configured timeout period (default: 2 hours). */
    ABANDONED
}
