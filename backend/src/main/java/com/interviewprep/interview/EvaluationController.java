package com.interviewprep.interview;

import com.interviewprep.config.AuthenticationContext;
import com.interviewprep.domain.*;
import com.interviewprep.question.AiGatewayService;
import jakarta.persistence.EntityManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller handling AI evaluation retry operations for failed answer evaluations.
 * <p>
 * When an AI-generated evaluation fails (e.g., due to a transient service error), this controller provides
 * an endpoint for the frontend to trigger a re-evaluation of the same answer. It locates the failed evaluation,
 * re-invokes the appropriate AI evaluation method based on question type, and persists the new successful result.
 * </p>
 * <h3>Retry Flow:</h3>
 * <ol>
 *   <li>Find the {@link Evaluation} with status {@link EvaluationStatus#FAILED} for the given answer.</li>
 *   <li>Verify the authenticated user owns this evaluation (via session ownership).</li>
 *   <li>Re-invoke AI evaluation using the same deterministic prompt.</li>
 *   <li>Mark the old failed evaluation as {@link EvaluationStatus#SUCCESS} and persist the new one.</li>
 * </ol>
 * 
 * @see AiGatewayService
 * @see Evaluation
 */
@RestController
@RequestMapping("/evaluation")
public class EvaluationController {

    /** The JPA entity manager used to load and persist domain entities. */
    private final EntityManager entityManager;
    
    /** Service for invoking AI-based answer evaluation (coding or theory). */
    private final AiGatewayService aiGatewayService;
    
    /** Repository for loading answers by ID. */
    private final AnswerRepository answerRepository;
    
    /** Repository for managing evaluations. */
    private final EvaluationRepository evaluationRepository;
    
    /** Provides the currently authenticated user's ID from the JWT token. */
    private final AuthenticationContext authContext;

    /**
     * Constructs the evaluation controller with all required dependencies.
     * 
     * @param entityManager          the JPA entity manager for database operations
     * @param aiGatewayService       the AI gateway service for re-evaluating answers
     * @param answerRepository       repository for loading answers
     * @param evaluationRepository   repository for managing evaluations
     * @param authContext            authentication context providing the current user's ID
     */
    public EvaluationController(EntityManager entityManager, AiGatewayService aiGatewayService,
                                AnswerRepository answerRepository, EvaluationRepository evaluationRepository,
                                AuthenticationContext authContext) {
        this.entityManager = entityManager;
        this.aiGatewayService = aiGatewayService;
        this.answerRepository = answerRepository;
        this.evaluationRepository = evaluationRepository;
        this.authContext = authContext;
    }

    /**
     * Retries a failed AI evaluation for a specific answer.
     * <p>
     * Finds the existing {@link Evaluation} with status {@link EvaluationStatus#FAILED}, verifies user ownership,
     * and re-invokes the appropriate AI evaluation method (coding or theory). On success, marks the old evaluation
     * as succeeded and persists the new evaluation result.
     * </p>
     * 
     * @param answerId the ID of the answer whose failed evaluation should be retried
     * @return the newly generated {@link Evaluation} on success; {@code badRequest} if no failed evaluation exists;
     *         {@code 403 Forbidden} if the user doesn't own this session; {@code 500 Internal Server Error} on AI failure
     */
    @PostMapping("/retry/{answerId}")
    public ResponseEntity<Evaluation> retryEvaluation(
            @PathVariable Long answerId) {

        Long userId = authContext.getCurrentUserId();

        // Find the existing failed evaluation for this answer
        java.util.List<Evaluation> evaluations = entityManager.createQuery(
                "SELECT e FROM Evaluation e WHERE e.answer.id = :answerId AND e.status = 'FAILED'",
                Evaluation.class).setParameter("answerId", answerId).getResultList();

        if (evaluations.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        Evaluation failedEval = evaluations.get(0);
        Answer answer = failedEval.getAnswer();

        // Verify the user owns this evaluation
        InterviewSession session = entityManager.find(InterviewSession.class, answer.getSession().getId());
        if (session == null || !session.getUser().getId().equals(userId)) {
            return ResponseEntity.status(403).body(null);
        }

        // Re-evaluate using the same deterministic prompt
        try {
            Evaluation newEvaluation;
            if (answer.getQuestion().getType() == QuestionType.CODE) {
                newEvaluation = aiGatewayService.evaluateCodingAnswer(answer.getQuestion(), answer);
            } else {
                newEvaluation = aiGatewayService.evaluateTheoryAnswer(answer.getQuestion(), answer);
            }

            // Mark the old failed evaluation as succeeded if retry worked
            failedEval.setStatus(EvaluationStatus.SUCCESS);

            // Persist the new, properly-scored evaluation to the database so it survives page refresh.
            entityManager.persist(newEvaluation);

            return ResponseEntity.ok(newEvaluation);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }
}
