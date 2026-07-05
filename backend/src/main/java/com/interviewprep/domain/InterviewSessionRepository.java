package com.interviewprep.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link InterviewSession} entity operations.
 * 
 * <p>This repository provides CRUD operations and custom query methods for managing interview sessions,
 * including finding active sessions by user, retrieving session history, and querying sessions by status.</p>
 * 
 * <h3>Key Operations:</h3>
 * <ul>
 *   <li>{@code findByStatusAndStartedAtBefore} — Find expired sessions for cleanup (used by {@code SessionTimeoutCleanupTask}).</li>
 *   <li>{@code findFirstByUserIdAndStatusOrderByStartedAtDesc} — Get the most recent active session for a user.</li>
 *   <li>{@code findByUserIdOrderByStartedAtDesc} — Retrieve all sessions for a user, ordered by start time (newest first).</li>
 * </ul>
 * 
 * @see InterviewSession
 */
@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {

    /**
     * Finds all interview sessions with the given status that started before the specified cutoff time.
     * 
     * <p>This method is used by {@code SessionTimeoutCleanupTask} to identify active sessions that have exceeded
     * their timeout period and should be marked as abandoned.</p>
     * 
     * @param status The session status to filter by (typically {@link SessionStatus#ACTIVE}).
     * @param startedAt The cutoff timestamp — sessions starting before this time are returned.
     * @return A list of interview sessions matching the criteria.
     */
    List<InterviewSession> findByStatusAndStartedAtBefore(SessionStatus status, LocalDateTime startedAt);

    /**
     * Finds an active interview session by its ID.
     * 
     * <p>This method is used to retrieve a specific session that is currently in progress.</p>
     * 
     * @param status The expected session status (typically {@link SessionStatus#ACTIVE}).
     * @param id The unique identifier of the session.
     * @return The matching interview session, or null if not found.
     */
    InterviewSession findByStatusAndId(SessionStatus status, Long id);

    /**
     * Finds the most recent active interview session for a given user.
     * 
     * <p>This query returns at most one session — the latest active session ordered by start time in descending order.</p>
     * 
     * @param userId The ID of the user whose active session to find.
     * @param status The expected session status (typically {@link SessionStatus#ACTIVE}).
     * @return An {@link Optional} containing the most recent active session if found, or empty otherwise.
     */
    @Query("SELECT s FROM InterviewSession s WHERE s.user.id = :userId AND s.status = 'ACTIVE' ORDER BY s.startedAt DESC LIMIT 1")
    Optional<InterviewSession> findFirstByUserIdAndStatusOrderByStartedAtDesc(Long userId, SessionStatus status);

    /**
     * Finds all interview sessions for a given user, ordered by start time (newest first).
     * 
     * <p>This method is used to retrieve the complete session history for analytics or display purposes.</p>
     * 
     * @param userId The ID of the user whose sessions to find.
     * @return A list of interview sessions belonging to the user, ordered by start time descending.
     */
    List<InterviewSession> findByUserIdOrderByStartedAtDesc(Long userId);
}
