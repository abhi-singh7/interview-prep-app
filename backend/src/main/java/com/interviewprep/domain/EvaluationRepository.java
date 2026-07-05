package com.interviewprep.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository for managing {@link Evaluation} entities.
 * <p>
 * Evaluations represent AI-generated assessments of user answers within an interview session.
 * Each evaluation contains a score (0-100), detailed feedback, and references to the
 * corresponding answer and question being evaluated.
 * </p>
 * <p>
 * This repository extends Spring Data JPA's {@link JpaRepository} to inherit standard CRUD operations
 * (save, findById, findAll, delete) and provides a custom query method for retrieving evaluations
 * ordered by score descending within a given session.
 * </p>
 *
 * @see Evaluation
 * @see AnswerRepository
 */
@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    /**
     * Retrieves all evaluations associated with a specific answer session, sorted by score in descending order.
     * <p>
     * This method is used to fetch the complete set of AI-generated evaluations for an interview session's answers,
     * allowing the frontend to display results ranked from highest to lowest score. The ordering helps users
     * quickly identify their strongest and weakest answers.
     * </p>
     *
     * @param sessionId the ID of the answer session whose evaluations should be retrieved
     * @return a list of {@link Evaluation} objects sorted by score descending; empty list if no evaluations exist
     */
    List<Evaluation> findByAnswerSessionIdOrderByScoreDesc(Long sessionId);
}
