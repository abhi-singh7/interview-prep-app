package com.interviewprep.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    // FIX-15: single fetch is all that's needed for the interview flow (without-vector query removed)
    java.util.List<Question> findBySessionId(Long sessionId);

    // My Sessions / Session Detail (restored from origin/main)
    long countBySessionId(Long sessionId);

    @Query("SELECT q FROM Question q WHERE q.session IS NULL AND q.categoryId = :categoryId")
    List<Question> findOrphansByCategory(@org.springframework.data.repository.query.Param("categoryId") String categoryId);
}
