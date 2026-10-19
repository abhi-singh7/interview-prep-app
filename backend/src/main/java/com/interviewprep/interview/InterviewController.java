package com.interviewprep.interview;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.interviewprep.config.AuthenticationContext;
import com.interviewprep.config.ParsingUtils;
import com.interviewprep.domain.*;
import com.interviewprep.embedding.EmbeddingService;
import com.interviewprep.question.AiGatewayService;
import com.interviewprep.service.TopicAutoSelectionService;
import jakarta.persistence.EntityManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/interview")
public class InterviewController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(InterviewController.class);
    // FIX-11: upper bound on questions per session (prevents unbounded AI generation)
    private static final int MAX_QUESTION_COUNT = 20;

    private final EntityManager entityManager;
    private final AiGatewayService aiGatewayService;
    private final EmbeddingService embeddingService;
    private final InterviewSessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final EvaluationRepository evaluationRepository;
    private final AuthenticationContext authContext;
    private final TopicAutoSelectionService topicAutoSelectionService;
    private final CategoryRepository categoryRepository;
    private final InterviewEvaluationService interviewEvaluationService;

    // FIX-16: reuse a single JsonMapper instance instead of building one per loop iteration
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    // FIX-16: memoize resolved topic names per categoryId string to avoid N+1 category lookups
    private final java.util.Map<String, List<String>> topicNamesCache = new java.util.HashMap<>();

    public InterviewController(EntityManager entityManager, AiGatewayService aiGatewayService, EmbeddingService embeddingService,
                               InterviewSessionRepository sessionRepository, QuestionRepository questionRepository,
                               AnswerRepository answerRepository, EvaluationRepository evaluationRepository,
                               AuthenticationContext authContext, TopicAutoSelectionService topicAutoSelectionService,
                               CategoryRepository categoryRepository, InterviewEvaluationService interviewEvaluationService) {
        this.entityManager = entityManager;
        this.aiGatewayService = aiGatewayService;
        this.embeddingService = embeddingService;
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.evaluationRepository = evaluationRepository;
        this.authContext = authContext;
        this.topicAutoSelectionService = topicAutoSelectionService;
        this.categoryRepository = categoryRepository;
        this.interviewEvaluationService = interviewEvaluationService;
    }

    // FIX-18: defensively map AI-provided question types to the enum. The model sometimes returns
    // free-form values (e.g. "THEORY_ONLY (conceptual focus)") that QuestionType.valueOf() rejects,
    // which crashed startInterview and orphaned the session. Treat anything mentioning CODE/CODING as
    // CODE; default every other non-blank value to THEORY so generation never throws mid-session.
    private static QuestionType parseQuestionType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim().toUpperCase();
        switch (t) {
            case "THEORY":
                return QuestionType.THEORY;
            case "CODE":
                return QuestionType.CODE;
        }
        if (t.contains("CODE") || t.contains("CODING")) {
            logger.warn("AI returned non-standard question type, mapping to CODE: {}", raw);
            return QuestionType.CODE;
        }
        logger.warn("AI returned non-standard question type, defaulting to THEORY: {}", raw);
        return QuestionType.THEORY;
    }

    @PostMapping("/start")
    public ResponseEntity<InterviewStartResponse> startInterview(
            @RequestBody InterviewStartRequest request) {

        // Validate all parameters are present
        if (request.getDifficulty() == null || request.getCount() <= 0) {
            return ResponseEntity.badRequest().body(null);
        }
        // FIX-11: bound question generation to avoid unbounded AI cost / sessions
        if (request.getCount() > MAX_QUESTION_COUNT) {
            throw new IllegalArgumentException("Cannot generate more than " + MAX_QUESTION_COUNT + " questions at once");
        }

        Long userId = authContext.getCurrentUserId();
        User user = entityManager.find(User.class, userId);
        
        // Resolve topic names for auto-select or validation
        List<String> resolvedTopicIds = resolveTopicIds(request.getTopicIds(), request.getLanguageId());
        
        // Validate all topicIds belong to the selected language if provided
        if (request.getLanguageId() != null && !request.getLanguageId().isBlank()) {
            validateTopicsForLanguage(resolvedTopicIds, request.getLanguageId());
        }

        InterviewSession session = new InterviewSession();
        session.setUser(user);
        session.setCategoryId(request.getLanguageId()); // Store language ID for backward compatibility
        session.setTopicIdsJson(serializeTopicIds(request.getTopicIds()));
        session.setDifficulty(request.getDifficulty());
        session.setLanguageId(request.getLanguageId() != null ? request.getLanguageId() : "");
        session.setStatus(SessionStatus.ACTIVE);
        session.setTimeoutHours(2);

        sessionRepository.save(session);
        java.util.List<Question> questions = new java.util.ArrayList<>();
        try {

        // Generate questions via AI gateway — pass topic names and language name for prompt generation
        List<String> topicNames = resolveTopicNames(resolvedTopicIds);

        
        // FIX: the old fallback injected a raw category *ID* (e.g. "42") as the prompt's
        // "Programming Language". The language is only known when languageId is set; otherwise
        // leave it blank — the generation prompt instructs the model to use Java then.
        String languageNameForPrompt = request.getLanguageId() != null && !request.getLanguageId().isBlank()
            ? resolveLanguageName(request.getLanguageId()) : "";
            
        // Only show "Category: X" when user explicitly selected topics; auto-select mode relies on language context alone
        String contextForDisplay = !topicNames.isEmpty() && request.getTopicIds() != null && !request.getTopicIds().isEmpty()
            ? resolveLanguageName(request.getLanguageId())  // User selected specific topics: show Category + Topics
            : "";  // Auto-select mode: no explicit category to show
            
        List<com.interviewprep.output.QuestionRecord> questionRecords = aiGatewayService.generateQuestions(
                contextForDisplay, request.getDifficulty(), request.getCount(), languageNameForPrompt, topicNames);
        
        for (var record : questionRecords) {
            Question q = new Question();
            q.setSession(session);
            q.setType(parseQuestionType(record.type()));
            // Use topic names from the record if available, resolve to IDs for categoryId storage
            if (record.topics() != null && !record.topics().isEmpty()) {
                q.setCategoryId(resolveTopicNamesToIds(record.topics()));
            } else {
                q.setCategoryId(null);
            }
            q.setQuestionText(record.questionText());
            if (record.title() != null) q.setTitle(record.title());
            if (record.description() != null) q.setDescription(record.description());
            if (record.codePrompt() != null) q.setCodePrompt(record.codePrompt());
            if (record.referenceAnswer() != null && !record.referenceAnswer().isBlank()) {
                q.setReferenceAnswer(record.referenceAnswer().trim());
            }
            
            // Generate vector embedding for the question text (Hibernate 7 native VECTOR type)
            String textToEmbed = record.questionText();
            if (textToEmbed != null && !textToEmbed.isBlank()) {
                try {
                    float[] embedding = embeddingService.embed(textToEmbed);
                    double[] doubleArray = new double[embedding.length];
                    for (int i = 0; i < embedding.length; i++) {
                        doubleArray[i] = (double) embedding[i];
                    }
                    q.setVectorEmbedding(doubleArray);
                } catch (Exception e) {
                    // FIX-17: leave the embedding null on failure rather than storing a zero vector
                    logger.warn("Failed to generate vector embedding for question: {} (leaving null)", textToEmbed, e);
                }
            }
            
            questions.add(q);
        }

        // Persist generated questions with session reference
        for (Question q : questions) {
            q.setSession(session);
            questionRepository.save(q);
        }
        } catch (Exception e) {
            // FIX-11: a gateway/valueOf failure must not leave an orphan ACTIVE session behind
            logger.error("Failed to generate interview questions for sessionId={}: {}", session.getId(), e.getMessage(), e);
            try {
                sessionRepository.delete(session);
            } catch (Exception ex) {
                logger.warn("Failed to delete orphan session {}: {}", session.getId(), ex.getMessage());
            }
            throw e;
        }

        InterviewStartResponse response = new InterviewStartResponse();
        response.setSessionId(session.getId());
        response.setQuestions(questions);
        return ResponseEntity.ok(response);
    }

    private List<String> resolveTopicIds(List<String> topicIds, String languageId) {
        if (topicIds == null || topicIds.isEmpty()) {
            // Auto-select topics from the selected language
            List<String> autoTopics = topicAutoSelectionService.autoSelectTopics(languageId);
            return autoTopics.stream()
                    .map(name -> findCategoryIdByNameAndParent(name, languageId))
                    .filter(Objects::nonNull)
                    .toList();
        }
        return topicIds;
    }

    private String findCategoryIdByNameAndParent(String name, String parentIdStr) {
        Long parentId = ParsingUtils.parseLongSafe(parentIdStr);
        if (parentId == null) return null;
        
        List<Category> topics = categoryRepository.findByParentIdAndNameContainingIgnoreCase(parentId, name);
        if (!topics.isEmpty()) {
            return String.valueOf(topics.getFirst().getId());
        }
        // Fallback: search all categories with this name (for standalone topics)
        return null;
    }

    private List<String> resolveTopicNames(List<String> topicIds) {
        if (topicIds == null || topicIds.isEmpty()) return List.of();
        
        List<Long> ids = topicIds.stream()
                .map(ParsingUtils::parseLongSafe)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
                
        if (ids.isEmpty()) return List.of();
        
        List<Category> categories = categoryRepository.findByIdIn(ids);
        return categories.stream()
                .map(Category::getName)
                .toList();
    }

    private void validateTopicsForLanguage(List<String> topicIds, String languageId) {
        Long parentId = ParsingUtils.parseLongSafe(languageId);
        if (parentId == null) return;
        
        List<Category> validTopics = categoryRepository.findByParentIdAndNameContainingIgnoreCase(parentId, "");
        List<Long> validIds = validTopics.stream()
                .map(Category::getId)
                .toList();
        
        for (String topicId : topicIds) {
            Long id = ParsingUtils.parseLongSafe(topicId);
            if (id != null && !validIds.contains(id)) {
                throw new IllegalArgumentException("Topic ID " + topicId + " does not belong to the selected language");
            }
        }
    }

    private String serializeTopicIds(List<String> topicIds) {
        if (topicIds == null || topicIds.isEmpty()) return "";
        return "{" + String.join(",", topicIds) + "}";
    }

    // Resolve comma-separated topic IDs from Question.categoryId to topic names for results display
    private List<String> resolveTopicNamesFromCategoryId(String categoryIdStr) {
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

    @GetMapping("/resume")
    public ResponseEntity<?> getResumeStatus() {
        Long userId = authContext.getCurrentUserId();

        // Find active session for this user using JPA
        Optional<InterviewSession> optionalSession = sessionRepository.findFirstByUserIdAndStatusOrderByStartedAtDesc(userId, SessionStatus.ACTIVE);
        if (optionalSession.isPresent()) {
            InterviewSession session = optionalSession.get();
            long sessionId = session.getId();
            String category = session.getCategoryId();

            // FIX-04: count distinct questions answered (rows == distinct once dedup is enforced)
            // FIX: include total question count so the resume card can render "x / total" instead of the old hardcoded /5
            long answeredCount = answerRepository.countDistinctQuestionsBySessionId(sessionId);
            long totalQuestions = questionRepository.countBySessionId(sessionId);

            ObjectNode resumeInfo = JsonMapper.builder().build().createObjectNode();
            resumeInfo.put("sessionId", sessionId);
            resumeInfo.put("categoryId", category);
            resumeInfo.put("difficulty", session.getDifficulty());
            resumeInfo.put("answeredQuestions", answeredCount);
            resumeInfo.put("totalQuestions", totalQuestions);

            return ResponseEntity.ok(resumeInfo);
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    @GetMapping("/session/{sessionId}/questions")
    public ResponseEntity<?> getQuestionsForSession(@PathVariable Long sessionId) {
        Long userId = authContext.getCurrentUserId();

        // Find active session for this user
        InterviewSession session = sessionRepository.findByStatusAndId(SessionStatus.ACTIVE, sessionId);
        if (session == null || !session.getUser().getId().equals(userId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Session not found or not active"));
        }

        try {
            // FIX-15: single query fetches the questions directly (diagnostic triple-query removed)
            List<Question> questions = questionRepository.findBySessionId(session.getId());
            if (questions.isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            // FIX-16: reuse the shared JsonMapper instance instead of building one per request
            ArrayNode questionsArray = jsonMapper.createArrayNode();
            for (Question q : questions) {
                ObjectNode qNode = jsonMapper.createObjectNode();
                qNode.put("id", q.getId());
                qNode.put("type", q.getType().name());
                qNode.put("categoryId", q.getCategoryId());
                qNode.put("questionText", q.getQuestionText());
                if (q.getTitle() != null) {
                    qNode.put("title", q.getTitle());
                }
                if (q.getDescription() != null) {
                    qNode.put("description", q.getDescription());
                }
                if (q.getCodePrompt() != null) {
                    qNode.put("codePrompt", q.getCodePrompt());
                }
                questionsArray.add(qNode);
            }

            return ResponseEntity.ok(questionsArray);
        } catch (org.springframework.orm.jpa.JpaSystemException e) {
            // Specific handling for pgvector-related JPA errors
            logger.error("JpaSystemError fetching questions for sessionId={}: {}", session.getId(), e.getMessage());
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                logger.error("  Root cause: {}", e.getCause().getMessage());
            }
            // Print full stack trace to identify the exact deserialization issue
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            logger.error("Full stack trace:\n{}", sw.toString());
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch questions"));
        } catch (Exception e) {
            logger.error("Error fetching questions for sessionId={}: {}", session.getId(), e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch questions"));
        }
    }

    @PostMapping("/finish")
    public ResponseEntity<?> finishInterview(
            @RequestBody InterviewFinishRequest request) {

        Long userId = authContext.getCurrentUserId();

        // Find active session for this user
        InterviewSession session = sessionRepository.findById(request.getSessionId()).orElse(null);
        if (session == null || !session.getUser().getId().equals(userId)) {
            return ResponseEntity.status(403).body(null);
        }
        if (session.getStatus() != SessionStatus.ACTIVE) {
            return ResponseEntity.badRequest().body(null);
        }

        session.setStatus(SessionStatus.COMPLETED);
        session.setEndedAt(java.time.LocalDateTime.now());
        sessionRepository.save(session);

        // Visibility guard: sessions 98/99 completed with zero stored answers and the gap was invisible.
        long totalQuestions = questionRepository.countBySessionId(session.getId());
        long submittedAnswers = answerRepository.findBySessionId(session.getId()).size();
        if (submittedAnswers < totalQuestions) {
            logger.warn("Session {} completed with only {}/{} answers submitted — unanswered questions get no evaluation",
                    session.getId(), submittedAnswers, totalQuestions);
        }

        // Kick off evaluation on a background thread and return immediately so the frontend can
        // navigate to /results and poll GET /results until every answer has an evaluation.
        interviewEvaluationService.evaluateSession(session.getId());

        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", session.getId());
        body.put("status", "EVALUATING");
        return ResponseEntity.accepted().body(body);
    }

    // Return the already-submitted answers for a session so the frontend can restore them on
    // resume/refresh (the in-memory answer maps are lost across page loads).
    @GetMapping("/session/{sessionId}/answers")
    public ResponseEntity<?> getAnswersForSession(@PathVariable Long sessionId) {
        Long userId = authContext.getCurrentUserId();
        InterviewSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null || !session.getUser().getId().equals(userId)) {
            return ResponseEntity.status(403).body(null);
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (Answer a : answerRepository.findBySessionId(sessionId)) {
            Map<String, Object> m = new HashMap<>();
            m.put("questionId", a.getQuestion().getId());
            m.put("answerText", a.getAnswerText());
            m.put("languageSubmitted", a.getLanguageSubmitted());
            out.add(m);
        }
        return ResponseEntity.ok(out);
    }

    @GetMapping("/results/{sessionId}")
    public ResponseEntity<?> getResults(@PathVariable Long sessionId) {
        Long userId = authContext.getCurrentUserId();

        // Find session for this user
        InterviewSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        SessionStatus status = session.getStatus();

        try {
            if (status == SessionStatus.COMPLETED) {
                // One-time backfill so legacy sessions still show correct answers in review
                ensureReferenceAnswers(session);
            }

            // Find all evaluations for this session's questions
            java.util.List<Evaluation> evaluationsList = evaluationRepository.findByAnswerSessionIdOrderByScoreDesc(session.getId());

            // FIX: fetch answers + questions once and share them with buildQuestionReviews
            // (the old code re-ran the same three queries inside it — results-page polls this endpoint up to 8 times).
            java.util.List<Answer> sessionAnswers = answerRepository.findBySessionId(session.getId());
            java.util.List<Question> sessionQuestions = questionRepository.findBySessionId(session.getId());
            long totalQuestions = sessionQuestions.size();
            long submittedAnswers = sessionAnswers.size();
            if (status == SessionStatus.COMPLETED && submittedAnswers < totalQuestions) {
                logger.warn("Session {} completed with only {}/{} answers submitted — unanswered questions get no evaluation",
                        session.getId(), submittedAnswers, totalQuestions);
            }

            double totalScore = 0;
            int evaluatedCount = 0;
            java.util.List<EvaluationDetail> evaluations = new java.util.ArrayList<>();

            for (Evaluation eval : evaluationsList) {
                if (eval == null || eval.getAnswer() == null) continue;

                Question question = eval.getAnswer().getQuestion();
                Answer answer = eval.getAnswer();

                EvaluationDetail detail = new EvaluationDetail();
                detail.setEvaluationId(eval.getId());
                detail.setAnswerId(answer.getId());
                detail.setQuestionType(question.getType());
                detail.setQuestionText(question.getQuestionText());
                detail.setScore(eval.getScore());
                detail.setStatus(eval.getStatus());
                detail.setIsCorrect(eval.getIsCorrect());
                detail.setCorrectnessExplanation(eval.getCorrectnessExplanation());
                detail.setTimeComplexity(eval.getTimeComplexity());
                detail.setSpaceComplexity(eval.getSpaceComplexity());

                // FAILED evaluations carry no AI payload (null JSONB fields); skip parsing for those
                java.util.List<String> strengths = new java.util.ArrayList<>();
                if (eval.getStatus() == EvaluationStatus.SUCCESS && eval.getStrengthsJson() != null) {
                    // FIX-16: shared mapper
                    ArrayNode strengthsArray = (ArrayNode) jsonMapper.readTree(eval.getStrengthsJson());
                    for (JsonNode sn : strengthsArray) {
                        strengths.add(sn.asText());
                    }
                }
                detail.setStrengths(strengths);

                java.util.List<String> weaknesses = new java.util.ArrayList<>();
                if (eval.getStatus() == EvaluationStatus.SUCCESS && eval.getWeaknessesJson() != null) {
                    // FIX-16: shared mapper
                    ArrayNode weaknessesArray = (ArrayNode) jsonMapper.readTree(eval.getWeaknessesJson());
                    for (JsonNode wn : weaknessesArray) {
                        weaknesses.add(wn.asText());
                    }
                }
                detail.setWeaknesses(weaknesses);

                detail.setImprovedAnswer(eval.getImprovedAnswer());
                detail.setUserAnswerText(answer.getAnswerText());
                if (question.getType() == QuestionType.CODE) {
                    detail.setSubmittedCode(answer.getAnswerText());
                }

                // FIX-16: memoize resolved topic names per categoryId to avoid N+1 category lookups
                detail.setTopicNames(topicNamesCache.computeIfAbsent(
                        question.getCategoryId(), this::resolveTopicNamesFromCategoryId));

                evaluations.add(detail);
                // Only successful evaluations contribute to the overall score; FAILED ones are listed so retry is reachable
                if (eval.getStatus() == EvaluationStatus.SUCCESS && eval.getScore() != null) {
                    totalScore += eval.getScore();
                    evaluatedCount++;
                }
            }

            double overallScore = evaluatedCount > 0 ? totalScore / evaluatedCount : 0;

            ResultsResponse response = new ResultsResponse();
            response.setSessionId(session.getId());
            response.setStatus(status);
            response.setOverallScore(Math.round(overallScore * 100.0) / 100.0);
            response.setEvaluations(evaluations);
            response.setQuestions(buildQuestionReviews(session, sessionQuestions, sessionAnswers, evaluationsList));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch results"));
        }
    }

    // Generate + persist a reference answer for any question still missing one (legacy sessions).
    // Questions that already carry one are skipped, so repeat calls become no-ops.
    private void ensureReferenceAnswers(InterviewSession session) {
        List<Question> qs = questionRepository.findBySessionId(session.getId());
        for (Question q : qs) {
            if (q.getReferenceAnswer() != null && !q.getReferenceAnswer().isBlank()) continue;
            try {
                String ref = aiGatewayService.generateReferenceAnswer(q);
                if (ref != null && !ref.isBlank()) {
                    q.setReferenceAnswer(ref.trim());
                    questionRepository.save(q);
                }
            } catch (Exception e) {
                logger.warn("Failed to generate reference answer for question {}: {}", q.getId(), e.getMessage());
            }
        }
    }

    // Build the per-question review list from already-fetched entities (no re-queries):
    // every session question with the user's submitted answer (or null when skipped) plus
    // the stored reference (correct) answer, so unanswered/wrongly answered questions remain
    // reviewable after finishing.
    private List<QuestionReviewDetail> buildQuestionReviews(InterviewSession session,
                                                             List<Question> sessionQuestions,
                                                             List<Answer> sessionAnswers,
                                                             List<Evaluation> sessionEvaluations) {
        Map<Long, Answer> answersByQuestion = new HashMap<>();
        for (Answer a : sessionAnswers) {
            if (a != null && a.getQuestion() != null) {
                answersByQuestion.put(a.getQuestion().getId(), a);
            }
        }

        Map<Long, Evaluation> evalsByAnswer = new HashMap<>();
        for (Evaluation e : sessionEvaluations) {
            if (e != null && e.getAnswer() != null) {
                evalsByAnswer.put(e.getAnswer().getId(), e);
            }
        }

        List<QuestionReviewDetail> reviews = new ArrayList<>();
        for (Question q : sessionQuestions) {
            QuestionReviewDetail r = new QuestionReviewDetail();
            r.setQuestionId(q.getId());
            r.setQuestionType(q.getType());
            r.setQuestionText(q.getQuestionText());
            if (q.getTitle() != null) r.setTitle(q.getTitle());
            if (q.getDescription() != null) r.setDescription(q.getDescription());
            if (q.getCodePrompt() != null) r.setCodePrompt(q.getCodePrompt());
            r.setReferenceAnswer(q.getReferenceAnswer());

            Answer a = answersByQuestion.get(q.getId());
            if (a != null) {
                r.setUserAnswerText(a.getAnswerText());
                if (a.getLanguageSubmitted() != null) r.setLanguage(a.getLanguageSubmitted().toLowerCase());
                if (q.getType() == QuestionType.CODE) r.setSubmittedCode(a.getAnswerText());
            }

            Evaluation e = a != null ? evalsByAnswer.get(a.getId()) : null;
            if (e != null) {
                r.setAnswerId(e.getAnswer().getId());
                r.setStatus(e.getStatus());
                if (e.getScore() != null) r.setScore(e.getScore());
                r.setIsCorrect(e.getIsCorrect());
                r.setCorrectnessExplanation(e.getCorrectnessExplanation());
                r.setTimeComplexity(e.getTimeComplexity());
                r.setSpaceComplexity(e.getSpaceComplexity());
                r.setImprovedAnswer(e.getImprovedAnswer());
                if (e.getStatus() == EvaluationStatus.SUCCESS) {
                    r.setStrengths(readStringArrayOrNull(e.getStrengthsJson()));
                    r.setWeaknesses(readStringArrayOrNull(e.getWeaknessesJson()));
                }
            }

            r.setTopicNames(topicNamesCache.computeIfAbsent(
                    q.getCategoryId(), this::resolveTopicNamesFromCategoryId));

            reviews.add(r);
        }
        return reviews;
    }

    private List<String> readStringArrayOrNull(String json) {
        if (json == null) return new ArrayList<>();
        try {
            ArrayNode arr = (ArrayNode) jsonMapper.readTree(json);
            List<String> out = new ArrayList<>();
            for (JsonNode n : arr) {
                out.add(n.asText());
            }
            return out;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // Resolve language ID to name for prompt display
    private String resolveLanguageName(String languageIdStr) {
        if (languageIdStr == null || languageIdStr.isBlank()) return "";
        Long id = ParsingUtils.parseLongSafe(languageIdStr);
        if (id == null) return languageIdStr;
        
        Optional<Category> langCategory = categoryRepository.findById(id);
        return langCategory.map(Category::getName).orElse(languageIdStr);
    }

    // Resolve topic names back to IDs for categoryId storage in Question entity
    private String resolveTopicNamesToIds(List<String> topicNames) {
        if (topicNames == null || topicNames.isEmpty()) return "";
        
        List<Long> ids = categoryRepository.findByType("TOPIC").stream()
            .filter(cat -> topicNames.contains(cat.getName()))
            .map(Category::getId)
            .collect(Collectors.toList());
            
        return String.join(",", ids.stream().map(String::valueOf).toList());
    }
}
