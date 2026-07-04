package com.interviewprep.interview;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;

import com.interviewprep.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service handling interview completion, evaluation aggregation, and results retrieval.
 * 
 * <p>Encapsulates the finish/results pipeline: marking sessions complete, evaluating answers via AI gateway,
 * parsing JSONB strengths/weaknesses into typed lists, computing weighted average scores, and assembling
 * {@link ResultsResponse} DTOs.</p>
 */
@Service
public class InterviewResultService {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private final EvaluationRepository evaluationRepository;
    private final QuestionRepository questionRepository;

    public InterviewResultService(EvaluationRepository evaluationRepository, QuestionRepository questionRepository) {
        this.evaluationRepository = evaluationRepository;
        this.questionRepository = questionRepository;
    }

    /**
     * Aggregates stored evaluations for a session into a ResultsResponse.
     * 
     * <p>Reads persisted {@link Evaluation} entities, parses JSONB strength/weakness arrays,
     * resolves topic names from category IDs, and computes the overall weighted-average score.</p>
     * 
     * @param sessionId the interview session ID to retrieve results for
     * @return a ResultsResponse with aggregated evaluations and computed score
     */
    @Transactional(readOnly = true)
    public ResultsResponse assembleResults(Long sessionId) {
        List<Evaluation> evaluationsList = evaluationRepository.findByAnswerSessionIdOrderByScoreDesc(sessionId);

        double totalScore = 0;
        int evaluatedCount = 0;
        long totalCount = questionRepository.countBySessionId(sessionId);
        List<EvaluationDetail> evaluations = new ArrayList<>();

        for (Evaluation eval : evaluationsList) {
            if (eval == null || eval.getAnswer() == null) continue;

            Question question = eval.getAnswer().getQuestion();
            Answer answer = eval.getAnswer();

            // Parse strengths and weaknesses from JSONB strings into lists.
            // The AI returns these as JSON arrays serialized to VARCHAR columns (strengthsJson, weaknessesJson).
            ArrayNode strengthsArray = (ArrayNode) JSON_MAPPER.readTree(eval.getStrengthsJson());
            ArrayNode weaknessesArray = (ArrayNode) JSON_MAPPER.readTree(eval.getWeaknessesJson());

            EvaluationDetail detail = buildEvaluationDetail(eval, question, answer);
            evaluations.add(detail);
            totalScore += eval.getScore();
            evaluatedCount++;
        }

        double overallScore = totalCount > 0 ? (evaluatedCount > 0 ? Math.round((totalScore / totalCount) * 100.0) / 100.0 : 0) : 0;

        // totalEarned: sum of all evaluated answer scores — used to display "earned / possible" format
        int totalEarned = evaluatedCount > 0 ? (int) Math.round(totalScore) : 0;

        ResultsResponse response = new ResultsResponse();
        response.setSessionId(sessionId);
        response.setStatus(SessionStatus.COMPLETED);
        response.setOverallScore(overallScore);
        response.setEvaluations(evaluations);
        response.setAnsweredCount(evaluatedCount);
        response.setTotalCount((int) totalCount);
        response.setTotalEarned(totalEarned);

        return response;
    }

    private EvaluationDetail buildEvaluationDetail(Evaluation eval, Question question, Answer answer) {
        EvaluationDetail detail = new EvaluationDetail();
        detail.setEvaluationId(eval.getId());
        detail.setQuestionType(question.getType());
        detail.setQuestionText(question.getQuestionText());
        detail.setScore(eval.getScore());
        detail.setStatus(eval.getStatus());
        detail.setIsCorrect(eval.getIsCorrect());
        detail.setCorrectnessExplanation(eval.getCorrectnessExplanation());
        detail.setTimeComplexity(eval.getTimeComplexity());
        detail.setSpaceComplexity(eval.getSpaceComplexity());

        java.util.List<String> strengths = parseJsonArrayToTextList(
                (ArrayNode) JSON_MAPPER.readTree(eval.getStrengthsJson()));
        detail.setStrengths(strengths);

        java.util.List<String> weaknesses = parseJsonArrayToTextList(
                (ArrayNode) JSON_MAPPER.readTree(eval.getWeaknessesJson()));
        detail.setWeaknesses(weaknesses);

        detail.setImprovedAnswer(eval.getImprovedAnswer());
        detail.setUserAnswerText(answer.getAnswerText());
        if (answer.getQuestion().getType() == QuestionType.CODE) {
            detail.setSubmittedCode(answer.getAnswerText());
        }

        return detail;
    }

    private List<String> parseJsonArrayToTextList(ArrayNode array) {
        List<String> result = new ArrayList<>();
        for (JsonNode node : array) {
            result.add(node.asText());
        }
        return result;
    }
}
