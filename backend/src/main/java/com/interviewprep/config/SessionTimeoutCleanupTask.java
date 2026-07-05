package com.interviewprep.config;

import com.interviewprep.domain.InterviewSession;
import com.interviewprep.domain.SessionStatus;
import com.interviewprep.domain.InterviewSessionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled task that periodically cleans up expired interview sessions.
 * 
 * <p>This component runs every 5 minutes to identify active interview sessions that have been running for more than
 * 2 hours without completion. Such sessions are marked as {@link SessionStatus#ABANDONED} with their end time recorded,
 * preventing stale data from accumulating in the database.</p>
 * 
 * <h3>Cleanup Logic:</h3>
 * <ol>
 *   <li>Finds all sessions with status {@code ACTIVE} that started more than 2 hours ago.</li>
 *   <li>Updates their status to {@link SessionStatus#ABANDONED}.</li>
 *   <li>Sets the {@code endedAt} timestamp to the current time.</li>
 * </ol>
 * 
 * <h3>Error Handling:</h3>
 * <p>If any exception occurs during cleanup (e.g., database connectivity issues), it is logged to stderr and does not
 * prevent subsequent scheduled runs from executing.</p>
 * 
 * @see InterviewSessionRepository#findByStatusAndStartedAtBefore(SessionStatus, java.time.LocalDateTime)
 */
@Component
public class SessionTimeoutCleanupTask {

    private final InterviewSessionRepository sessionRepository;

    /** The number of hours after which an active session is considered abandoned. */
    private static final int SESSION_TIMEOUT_HOURS = 2;

    /**
     * Constructs the cleanup task with the interview session repository.
     *
     * @param sessionRepository The repository used to query and update interview sessions.
     */
    public SessionTimeoutCleanupTask(InterviewSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * Cleans up expired active interview sessions by marking them as abandoned.
     * 
     * <p>This method is scheduled to run every 5 minutes via Spring's {@code @Scheduled} annotation.
     * It finds all ACTIVE sessions that have been running for more than 2 hours and transitions them
     * to the ABANDONED state.</p>
     */
    @Scheduled(fixedRate = 300000) // Run every 5 minutes (300,000 ms)
    @Transactional
    public void cleanupExpiredSessions() {
        // Calculate the cutoff time: any session started before this is considered expired
        java.time.LocalDateTime cutoffTime = java.time.LocalDateTime.now().minusHours(SESSION_TIMEOUT_HOURS);

        try {
            // Find all active sessions that have exceeded the timeout threshold
            sessionRepository.findByStatusAndStartedAtBefore(SessionStatus.ACTIVE, cutoffTime)
                    .forEach(session -> {
                        // Mark the session as abandoned and record its end time
                        session.setStatus(SessionStatus.ABANDONED);
                        session.setEndedAt(java.time.LocalDateTime.now());
                    });
        } catch (Exception e) {
            System.err.println("Failed to clean up expired sessions: " + e.getMessage());
        }
    }
}
