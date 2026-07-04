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
import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/interview")
public class AnswerSubmissionController {

    private final EntityManager entityManager;
    private final AuthenticationContext authContext;

    public AnswerSubmissionController(EntityManager entityManager, AuthenticationContext authContext) {
        this.entityManager = entityManager;
        this.authContext = authContext;
    }

    @PostMapping("/{sessionId}/answer")
    @Transactional
    public ResponseEntity<Void> submitAnswer(
            @PathVariable Long sessionId,
            @Valid @RequestBody AnswerRequest request) {

        Long userId = authContext.getCurrentUserId();
        InterviewSession session = entityManager.find(InterviewSession.class, sessionId);

        if (session == null || !session.getUser().getId().equals(userId) ||
                session.getStatus() != SessionStatus.ACTIVE) {
            return ResponseEntity.badRequest().build();
        }

        Answer answer = new Answer();
        answer.setSession(session);
        Question question = entityManager.find(Question.class, request.getQuestionId());
        if (question == null) {
            return ResponseEntity.badRequest().build();
        }
        answer.setQuestion(question);
        answer.setAnswerText(request.getAnswerText());

        // For CODE questions, languageSubmitted is required; for THEORY it's optional.
        String lang = request.getLanguageSubmitted();
        if (question.getType() == QuestionType.CODE && (lang == null || lang.isBlank())) {
            return ResponseEntity.badRequest().build();
        }
        answer.setLanguageSubmitted(lang);

        entityManager.persist(answer);
        return ResponseEntity.ok().build();
    }
}
