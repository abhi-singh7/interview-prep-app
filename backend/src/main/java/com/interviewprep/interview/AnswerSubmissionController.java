package com.interviewprep.interview;

import com.interviewprep.config.AuthenticationContext;
import com.interviewprep.domain.*;
import jakarta.persistence.EntityManager;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/interview")
public class AnswerSubmissionController {

    private final EntityManager entityManager;
    private final AuthenticationContext authContext;
    private final AnswerRepository answerRepository;

    public AnswerSubmissionController(EntityManager entityManager, AuthenticationContext authContext,
                                      AnswerRepository answerRepository) {
        this.entityManager = entityManager;
        this.authContext = authContext;
        this.answerRepository = answerRepository;
    }

    @PostMapping("/{sessionId}/answer")
    @Transactional
    public ResponseEntity<Void> submitAnswer(
            @PathVariable Long sessionId,
            @RequestBody AnswerRequest request) {

        Long userId = authContext.getCurrentUserId();
        InterviewSession session = entityManager.find(InterviewSession.class, sessionId);

        if (session == null || !session.getUser().getId().equals(userId) ||
                session.getStatus() != SessionStatus.ACTIVE) {
            return ResponseEntity.badRequest().build();
        }

        Question question = entityManager.find(Question.class, request.getQuestionId());

        // FIX-05: reject answers whose question does not belong to this session
        if (question == null || !question.getSession().getId().equals(session.getId())) {
            return ResponseEntity.status(403).build();
        }

        // FIX-14: persist only non-blank answer text (wastes AI calls / skews stats otherwise)
        if (request.getAnswerText() == null || request.getAnswerText().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // FIX-04: reject duplicate answers for the same session+question
        // (backstop to the DB UNIQUE(session_id, question_id) constraint)
        Answer existing = answerRepository.findBySessionIdAndQuestionId(session.getId(), question.getId());
        if (existing != null) {
            return ResponseEntity.status(409).build();
        }

        Answer answer = new Answer();
        answer.setSession(session);
        answer.setQuestion(question);
        answer.setAnswerText(request.getAnswerText());
        answer.setLanguageSubmitted(request.getLanguageSubmitted());

        entityManager.persist(answer);
        return ResponseEntity.ok().build();
    }
}
