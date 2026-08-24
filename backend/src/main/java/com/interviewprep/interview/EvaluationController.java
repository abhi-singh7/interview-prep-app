package com.interviewprep.interview;

import com.interviewprep.config.AuthenticationContext;
import com.interviewprep.config.ParsingUtils;
import com.interviewprep.domain.*;
import com.interviewprep.question.AiGatewayService;
import jakarta.persistence.EntityManager;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/evaluation")
public class EvaluationController {

    private final EntityManager entityManager;
    private final AiGatewayService aiGatewayService;
    private final AnswerRepository answerRepository;
    private final AuthenticationContext authContext;
    private final EvaluationRepository evaluationRepository;
    private final CategoryRepository categoryRepository;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public EvaluationController(EntityManager entityManager, AiGatewayService aiGatewayService,
                                AnswerRepository answerRepository,
                                AuthenticationContext authContext,
                                EvaluationRepository evaluationRepository,
                                CategoryRepository categoryRepository) {
        this.entityManager = entityManager;
        this.aiGatewayService = aiGatewayService;
        this.answerRepository = answerRepository;
        this.authContext = authContext;
        this.evaluationRepository = evaluationRepository;
        this.categoryRepository = categoryRepository;
    }

    @PostMapping("/retry/{answerId}")
    @Transactional
    public ResponseEntity<EvaluationDetail> retryEvaluation(
            @PathVariable Long answerId) {

        Long userId = authContext.getCurrentUserId();

        // Find the existing failed evaluation for this answer
        List<Evaluation> evaluations = entityManager.createQuery(
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

        try {
            Evaluation newEvaluation;
            if (answer.getQuestion().getType() == QuestionType.CODE) {
                newEvaluation = aiGatewayService.evaluateCodingAnswer(answer.getQuestion(), answer);
            } else {
                newEvaluation = aiGatewayService.evaluateTheoryAnswer(answer.getQuestion(), answer);
            }

            // Supersede the failed row: delete it and persist the successful one, so GET /results
            // shows exactly one evaluation per answer (UNIQUE on answer_id)
            evaluationRepository.delete(failedEval);
            entityManager.flush();
            Evaluation persisted = evaluationRepository.save(newEvaluation);

            return ResponseEntity.ok(toDetail(persisted));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    // Build a display detail from a saved evaluation, mirroring the results response shape
    private EvaluationDetail toDetail(Evaluation eval) {
        Answer answer = eval.getAnswer();
        Question question = answer.getQuestion();

        EvaluationDetail detail = new EvaluationDetail();
        detail.setEvaluationId(eval.getId());
        detail.setAnswerId(answer.getId());
        detail.setQuestionType(question.getType());
        detail.setQuestionText(question.getQuestionText());
        detail.setStatus(eval.getStatus());
        detail.setScore(eval.getScore());
        detail.setIsCorrect(eval.getIsCorrect());
        detail.setCorrectnessExplanation(eval.getCorrectnessExplanation());
        detail.setTimeComplexity(eval.getTimeComplexity());
        detail.setSpaceComplexity(eval.getSpaceComplexity());

        List<String> strengths = new ArrayList<>();
        if (eval.getStrengthsJson() != null) {
            ArrayNode arr = (ArrayNode) jsonMapper.readTree(eval.getStrengthsJson());
            for (JsonNode n : arr) {
                strengths.add(n.asText());
            }
        }
        detail.setStrengths(strengths);

        List<String> weaknesses = new ArrayList<>();
        if (eval.getWeaknessesJson() != null) {
            ArrayNode arr = (ArrayNode) jsonMapper.readTree(eval.getWeaknessesJson());
            for (JsonNode n : arr) {
                weaknesses.add(n.asText());
            }
        }
        detail.setWeaknesses(weaknesses);

        detail.setImprovedAnswer(eval.getImprovedAnswer());
        detail.setUserAnswerText(answer.getAnswerText());
        if (question.getType() == QuestionType.CODE) {
            detail.setSubmittedCode(answer.getAnswerText()); // Code is also in answerText for coding questions
        }

        detail.setTopicNames(resolveTopicNames(question.getCategoryId()));
        return detail;
    }

    // Resolve comma-separated topic IDs to names for display (same logic as InterviewController)
    private List<String> resolveTopicNames(String categoryIdStr) {
        if (categoryIdStr == null || categoryIdStr.isBlank()) return List.of();

        String[] ids = categoryIdStr.split(",");
        java.util.List<Long> idList = new java.util.ArrayList<>();
        for (String id : ids) {
            String trimmed = id.trim();
            if (!trimmed.isEmpty()) {
                Long parsedId = ParsingUtils.parseLongSafe(trimmed);
                if (parsedId != null) {
                    idList.add(parsedId);
                }
            }
        }

        if (idList.isEmpty()) return List.of();
        return categoryRepository.findByIdIn(idList).stream()
                .map(Category::getName)
                .toList();
    }
}
