package com.interviewprep.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {

    List<InterviewSession> findByStatusAndStartedAtBefore(SessionStatus status, LocalDateTime startedAt);

    InterviewSession findByStatusAndId(SessionStatus status, Long id);

    @Query("SELECT s FROM InterviewSession s WHERE s.user.id = :userId AND s.status = 'ACTIVE' ORDER BY s.startedAt DESC LIMIT 1")
    Optional<InterviewSession> findFirstByUserIdAndStatusOrderByStartedAtDesc(Long userId, SessionStatus status);

    List<InterviewSession> findByUserIdOrderByStartedAtDesc(Long userId);

}

