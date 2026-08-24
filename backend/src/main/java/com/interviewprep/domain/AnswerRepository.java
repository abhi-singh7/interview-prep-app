package com.interviewprep.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {
    List<Answer> findBySessionId(Long sessionId);

    // FIX-04/FIX-05: find an existing answer for a specific session+question (dedup + cross-session guard)
    Answer findBySessionIdAndQuestionId(Long sessionId, Long questionId);

    // FIX-04: count distinct questions answered in a session (rows == distinct after dedup, but robust regardless)
    long countDistinctQuestionsBySessionId(Long sessionId);
}
