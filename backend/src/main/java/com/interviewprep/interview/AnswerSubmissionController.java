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

/**
 * REST controller handling user answer submissions during an active interview session.
 * <p>
 * This controller receives {@link AnswerRequest} payloads from the frontend when a user submits their response
 * to a question within an ongoing interview. It validates that the session is still active, the user owns it,
 * and the question exists before persisting the answer to the database.
 * </p>
 * <h3>Validation Rules:</h3>
 * <ul>
 *   <li>The session must exist, belong to the authenticated user, and have status {@link SessionStatus#ACTIVE}.</li>
 *   <li>The referenced question must exist in the database.</li>
 *   <li>CODING questions require a non-blank {@code languageSubmitted}; THEORY questions do not.</li>
 * </ul>
 * 
 * @see AnswerRequest
 * @see InterviewSession
 */
@RestController
@RequestMapping("/interview")
public class AnswerSubmissionController {

    /** The JPA entity manager used to load and persist domain entities. */
    private final EntityManager entityManager;
    
    /** Provides the currently authenticated user's ID from the JWT token. */
    private final AuthenticationContext authContext;

    /**
     * Constructs the answer submission controller with the required dependencies.
     * 
     * @param entityManager   the JPA entity manager for database operations
     * @param authContext     the authentication context providing the current user's ID
     */
    public AnswerSubmissionController(EntityManager entityManager, AuthenticationContext authContext) {
        this.entityManager = entityManager;
        this.authContext = authContext;
    }

    /**
     * Submits a user's answer for a question within an active interview session.
     * <p>
     * Validates the session state (active, owned by current user), verifies the question exists,
     * and enforces language requirements based on question type. The answer is persisted as a new
     * {@link Answer} entity linked to both the session and the question.
     * </p>
     * 
     * @param sessionId   the ID of the active interview session
     * @param request     the validated answer submission containing question ID, text, and language
     * @return {@code ResponseEntity.ok()} on successful submission; {@code badRequest} if validation fails
     */
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
