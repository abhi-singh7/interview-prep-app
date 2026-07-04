package com.interviewprep.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    java.util.List<Question> findBySessionId(Long sessionId);

    // Questions with null session_id but belonging to a category — orphaned from before the FK fix.
    @Query("SELECT q FROM Question q WHERE q.session IS NULL AND q.categoryId = :categoryId")
    java.util.List<Question> findOrphansByCategory(@Param("categoryId") String categoryId);
    
    // Diagnostic: Count questions without loading vector embedding column
    @Query("SELECT COUNT(q) FROM Question q WHERE q.session.id = :sessionId")
    long countBySessionId(@Param("sessionId") Long sessionId);
    
    // Diagnostic: Fetch questions excluding the vector_embedding column to isolate pgvector issue
    @Query(value = "SELECT id, category_id, code_prompt, description, question_text, session_id, title, type FROM questions WHERE session_id = :sessionId", nativeQuery = true)
    java.util.List<Object[]> findBySessionIdWithoutVector(@Param("sessionId") Long sessionId);
}
