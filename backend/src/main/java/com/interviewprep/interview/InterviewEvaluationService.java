package com.interviewprep.interview;

import java.util.List;

import com.interviewprep.domain.Answer;
import com.interviewprep.domain.Evaluation;
import com.interviewprep.domain.EvaluationStatus;
import com.interviewprep.domain.Question;
import com.interviewprep.domain.QuestionType;
import com.interviewprep.question.AiGatewayService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Evaluates all of a session's answers on a background thread so that POST /finish can
 * return immediately and let the frontend navigate to /results and poll GET /results
 * until every answer has an evaluation. @Transactional gives each run its own persistence
 * context so lazy Answer.question associations resolve correctly in the worker thread.
 */
@Service
public class InterviewEvaluationService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(InterviewEvaluationService.class);

    private final AiGatewayService aiGatewayService;
    private final com.interviewprep.domain.AnswerRepository answerRepository;
    private final com.interviewprep.domain.EvaluationRepository evaluationRepository;

    public InterviewEvaluationService(AiGatewayService aiGatewayService,
                                      com.interviewprep.domain.AnswerRepository answerRepository,
                                      com.interviewprep.domain.EvaluationRepository evaluationRepository) {
        this.aiGatewayService = aiGatewayService;
        this.answerRepository = answerRepository;
        this.evaluationRepository = evaluationRepository;
    }

    @Async
    @Transactional
    public void evaluateSession(Long sessionId) {
        List<Answer> answers = answerRepository.findBySessionId(sessionId);
        for (Answer answer : answers) {
            try {
                Question question = answer.getQuestion();
                Evaluation evaluation;
                if (question.getType() == QuestionType.CODE) {
                    evaluation = aiGatewayService.evaluateCodingAnswer(question, answer);
                } else {
                    evaluation = aiGatewayService.evaluateTheoryAnswer(question, answer);
                }
                evaluationRepository.save(evaluation);
            } catch (Exception e) {
                logger.error("Evaluation failed for answerId={}: {}", answer.getId(), e.getMessage());
                Evaluation failed = new Evaluation();
                failed.setAnswer(answer);
                failed.setStatus(EvaluationStatus.FAILED);
                try {
                    evaluationRepository.save(failed);
                } catch (Exception ignore) {
                    // An evaluation may already exist for this answer; nothing else to do.
                }
            }
        }
    }
}
