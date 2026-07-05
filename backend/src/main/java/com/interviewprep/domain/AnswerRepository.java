package com.interviewprep.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Spring Data JPA repository for {@link Answer} entity operations.
 * 
 * <p>This repository provides CRUD operations and custom query methods for managing user answers to interview questions.</p>
 * 
 * <h3>Key Operations:</h3>
 * <ul>
 *   <li>{@code findBySessionId(Long)} — Retrieve all answers submitted during a specific interview session.</li>
 * </ul>
 * 
 * @see Answer
 */
@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {
    
    /**
     * Finds all answers associated with a given interview session.
     * 
     * <p>This method is used to retrieve the complete set of user responses for a particular mock interview.</p>
     * 
     * @param sessionId The unique identifier of the interview session.
     * @return A list of answers submitted during the specified session.
     */
    List<Answer> findBySessionId(Long sessionId);
}
