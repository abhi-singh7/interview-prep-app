package com.interviewprep.config;

import com.interviewprep.domain.InterviewSession;
import com.interviewprep.domain.SessionStatus;
import com.interviewprep.domain.InterviewSessionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SessionTimeoutCleanupTask {

    private final InterviewSessionRepository sessionRepository;

    public SessionTimeoutCleanupTask(InterviewSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    @Transactional
    public void cleanupExpiredSessions() {
        java.time.LocalDateTime cutoffTime = java.time.LocalDateTime.now().minusHours(2);

        try {
            sessionRepository.findByStatusAndStartedAtBefore(SessionStatus.ACTIVE, cutoffTime)
                    .forEach(session -> {
                        session.setStatus(SessionStatus.ABANDONED);
                        session.setEndedAt(java.time.LocalDateTime.now());
                    });
        } catch (Exception e) {
            System.err.println("Failed to clean up expired sessions: " + e.getMessage());
        }
    }
}
