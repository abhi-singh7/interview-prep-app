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

@RestController
@RequestMapping("/evaluation")
public class EvaluationController {

    private final EntityManager entityManager;
    private final AiGatewayService aiGatewayService;
    private final AnswerRepository answerRepository;
    private final EvaluationRepository evaluationRepository;
    private final AuthenticationContext authContext;

    public EvaluationController(EntityManager entityManager, AiGatewayService aiGatewayService,
                                AnswerRepository answerRepository, EvaluationRepository evaluationRepository,
                                AuthenticationContext authContext) {
        this.entityManager = entityManager;
        this.aiGatewayService = aiGatewayService;
        this.answerRepository = answerRepository;
        this.evaluationRepository = evaluationRepository;
        this.authContext = authContext;
    }

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
