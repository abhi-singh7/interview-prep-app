package com.interviewprep.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    // FIX-15: single fetch is all that's needed now (diagnostic count/without-vector queries removed)
    java.util.List<Question> findBySessionId(Long sessionId);
}
