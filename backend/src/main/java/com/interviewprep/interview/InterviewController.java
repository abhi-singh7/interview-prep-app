package com.interviewprep.interview;

import com.interviewprep.config.AuthenticationContext;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.interviewprep.domain.*;
import com.interviewprep.question.AiGatewayService;

import java.util.List;
import java.util.Map;

/**
 * REST controller managing the interview lifecycle: start, answer, finish, and results.
 * Delegates to InterviewSetupService (session/topic), QuestionGenerationService (AI/embedding),
 * and InterviewResultService (evaluation/scoring). Auth via JWT cookie enforced by JwtAuthenticationFilter.
 */
@RestController
@RequestMapping("/interview")
public class InterviewController {

    private final InterviewSetupService setup;
    private final QuestionGenerationService questions;
    private final InterviewResultService results;
    private final AuthenticationContext auth;
    private final InterviewSessionRepository sessions;
    private final AnswerRepository answers;
    private final AiGatewayService ai;
    private final EvaluationRepository evaluations;
    private final QuestionRepository questionRepo;

    public InterviewController(InterviewSetupService setup, QuestionGenerationService questions,
                               InterviewResultService results, AuthenticationContext auth,
                               InterviewSessionRepository sessions, AnswerRepository answers,
                               AiGatewayService ai, EvaluationRepository evaluations, QuestionRepository q) {
        this.setup = setup; this.questions = questions; this.results = results;
        this.auth = auth; this.sessions = sessions; this.answers = answers;
        this.ai = ai; this.evaluations = evaluations; this.questionRepo = q;
    }

    @PostMapping("/start")
    public ResponseEntity<InterviewStartResponse> startInterview(@Valid @RequestBody InterviewStartRequest req) {
        if (req.getDifficulty() == null || req.getCount() <= 0) return ResponseEntity.badRequest().body(null);
        InterviewSession session = setup.createSession(req);
        sessions.save(session);
        List<String> topicNames = setup.resolveTopicNames(
            req.getTopicIds() != null ? req.getTopicIds() : List.of());
        String langName = (req.getLanguageId() != null && !req.getLanguageId().isBlank())
            ? setup.resolveLanguageName(req.getLanguageId()) : "";
        List<Question> qList = questions.generateAndPersistQuestions(session.getId(), topicNames, langName, req.getDifficulty(), req.getCount());
        InterviewStartResponse resp = new InterviewStartResponse();
        resp.setSessionId(session.getId()); resp.setQuestions(qList);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/resume")
    public ResponseEntity<InterviewSummary> getResumeStatus() {
        return setup.getResumeStatus().map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/session/{sessionId}/questions")
    public ResponseEntity<?> getQuestionsForSession(@PathVariable Long sessionId) {
        Long userId = auth.getCurrentUserId();
        InterviewSession session = sessions.findByStatusAndId(SessionStatus.ACTIVE, sessionId);
        if (session == null || !session.getUser().getId().equals(userId))
            return ResponseEntity.badRequest().body(Map.of("error", "Session not found or not active"));
        List<Question> qs = questionRepo.findBySessionId(session.getId());
        if (qs.isEmpty()) return ResponseEntity.noContent().build();
        tools.jackson.databind.json.JsonMapper mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        tools.jackson.databind.node.ArrayNode arr = mapper.createArrayNode();
        for (Question q : qs) {
            tools.jackson.databind.node.ObjectNode n = mapper.createObjectNode();
            n.put("id", q.getId()); n.put("type", q.getType().name());
            n.put("categoryId", q.getCategoryId()); n.put("questionText", q.getQuestionText());
            if (q.getTitle() != null) n.put("title", q.getTitle());
            if (q.getDescription() != null) n.put("description", q.getDescription());
            if (q.getCodePrompt() != null) n.put("codePrompt", q.getCodePrompt());
            arr.add(n);
        }
        return ResponseEntity.ok(arr);
    }

    @PostMapping("/finish")
    public ResponseEntity<ResultsResponse> finishInterview(@RequestBody InterviewFinishRequest req) {
        Long userId = auth.getCurrentUserId();
        InterviewSession session = sessions.findById(req.getSessionId()).orElse(null);
        if (session == null || !session.getUser().getId().equals(userId)) return ResponseEntity.status(403).body(null);
        if (session.getStatus() != SessionStatus.ACTIVE) return ResponseEntity.badRequest().body(null);
        session.setStatus(SessionStatus.COMPLETED);
        session.setEndedAt(java.time.LocalDateTime.now());
        sessions.save(session);
        for (Answer answer : answers.findBySessionId(session.getId())) {
            try {
                Evaluation ev = (answer.getQuestion().getType() == QuestionType.CODE)
                    ? ai.evaluateCodingAnswer(answer.getQuestion(), answer)
                    : ai.evaluateTheoryAnswer(answer.getQuestion(), answer);
                evaluations.save(ev);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(InterviewController.class)
                    .warn("Failed to evaluate answer {} for question {}: {}", answer.getId(), answer.getQuestion().getId(), e.getMessage());
            }
        }
        return ResponseEntity.ok(results.assembleResults(session.getId()));
    }

    @GetMapping("/results/{sessionId}")
    public ResponseEntity<?> getResults(@PathVariable Long sessionId) {
        InterviewSession session = sessions.findById(sessionId).orElse(null);
        if (session == null) return ResponseEntity.notFound().build();
        try {
            ResultsResponse response = results.assembleResults(sessionId);
            response.setLanguageId(session.getLanguageId());
            response.setLanguageName(setup.resolveLanguageName(session.getLanguageId()));
            response.setTopicNames(setup.resolveTopicNames(
                java.util.List.of(session.getTopicIdsJson().replaceAll("[{}]", "").split(","))));
            return ResponseEntity.ok(response);
        }
        catch (Exception e) { return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch results")); }
    }
}
