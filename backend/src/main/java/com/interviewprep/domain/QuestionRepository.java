package com.interviewprep.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Question} entity operations.
 * 
 * <p>This repository provides CRUD operations and custom query methods for managing interview questions,
 * including diagnostic queries for troubleshooting vector embedding issues.</p>
 * 
 * <h3>Key Operations:</h3>
 * <ul>
 *   <li>{@code findBySessionId(Long)} — Retrieve all questions generated during a specific session.</li>
 *   <li>{@code findOrphansByCategory(String)} — Find orphaned questions (before FK constraint was added).</li>
 *   <li>{@code countBySessionId(Long)} — Count questions per session for diagnostic purposes.</li>
 *   <li>{@code findBySessionIdWithoutVector(Long)} — Fetch questions excluding vector embedding column.</li>
 * </ul>
 * 
 * @see Question
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    
    /**
     * Finds all questions associated with a given interview session.
     * 
     * <p>This method is used to retrieve the complete set of questions generated during a mock interview.</p>
     * 
     * @param sessionId The unique identifier of the interview session.
     * @return A list of questions belonging to the specified session.
     */
    java.util.List<Question> findBySessionId(Long sessionId);

    /**
     * Finds orphaned questions that have no associated session but belong to a specific category.
     * 
     * <p>This query identifies questions created before the foreign key constraint on {@code session_id} was added,
     * which may need cleanup or migration.</p>
     * 
     * @param categoryId The category ID to filter orphaned questions by.
     * @return A list of orphaned questions belonging to the specified category.
     */
    @Query("SELECT q FROM Question q WHERE q.session IS NULL AND q.categoryId = :categoryId")
    java.util.List<Question> findOrphansByCategory(@Param("categoryId") String categoryId);
    
    /**
     * Counts the number of questions associated with a given session.
     * 
     * <p>This diagnostic query is used to verify question counts without loading the full entity data,
     * particularly useful for troubleshooting pgvector embedding issues.</p>
     * 
     * @param sessionId The unique identifier of the interview session.
     * @return The count of questions belonging to the specified session.
     */
    @Query("SELECT COUNT(q) FROM Question q WHERE q.session.id = :sessionId")
    long countBySessionId(@Param("sessionId") Long sessionId);
    
    /**
     * Fetches questions for a session excluding the vector embedding column.
     * 
     * <p>This native query is used for diagnostic purposes to isolate pgvector-related issues by fetching
     * question data without loading the potentially problematic {@code vector_embedding} column.</p>
     * 
     * @param sessionId The unique identifier of the interview session.
     * @return A list of object arrays containing question fields (id, category_id, code_prompt, description, question_text, session_id, title, type).
     */
    @Query(value = "SELECT id, category_id, code_prompt, description, question_text, session_id, title, type FROM questions WHERE session_id = :sessionId", nativeQuery = true)
    java.util.List<Object[]> findBySessionIdWithoutVector(@Param("sessionId") Long sessionId);
}
