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

    public InterviewController(EntityManager entityManager, AiGatewayService aiGatewayService, EmbeddingService embeddingService,
                               InterviewSessionRepository sessionRepository, QuestionRepository questionRepository,
                               AnswerRepository answerRepository, EvaluationRepository evaluationRepository,
                               AuthenticationContext authContext, TopicAutoSelectionService topicAutoSelectionService,
                               CategoryRepository categoryRepository) {
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
    }

    @PostMapping("/start")
    public ResponseEntity<InterviewStartResponse> startInterview(
            @RequestBody InterviewStartRequest request) {

        // Validate all parameters are present
        if (request.getDifficulty() == null || request.getCount() <= 0) {
            return ResponseEntity.badRequest().body(null);
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

        // Generate questions via AI gateway — pass topic names and language name for prompt generation
        List<String> topicNames = resolveTopicNames(resolvedTopicIds);

        
        String languageNameForPrompt = request.getLanguageId() != null && !request.getLanguageId().isBlank() 
            ? resolveLanguageName(request.getLanguageId()) : (resolvedTopicIds.isEmpty() ? "" : resolvedTopicIds.getFirst());
            
        // Only show "Category: X" when user explicitly selected topics; auto-select mode relies on language context alone
        String contextForDisplay = !topicNames.isEmpty() && request.getTopicIds() != null && !request.getTopicIds().isEmpty()
            ? resolveLanguageName(request.getLanguageId())  // User selected specific topics: show Category + Topics
            : "";  // Auto-select mode: no explicit category to show
            
        List<com.interviewprep.output.QuestionRecord> questionRecords = aiGatewayService.generateQuestions(
                contextForDisplay, request.getDifficulty(), request.getCount(), languageNameForPrompt, topicNames);
        
        java.util.List<Question> questions = new java.util.ArrayList<>();
        for (var record : questionRecords) {
            Question q = new Question();
            q.setSession(session);
            q.setType(record.type() != null ? QuestionType.valueOf(record.type()) : null);
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
                    logger.warn("Failed to generate vector embedding for question: {}", textToEmbed, e);
                    q.setVectorEmbedding(new double[768]);
                }
            } else {
                q.setVectorEmbedding(new double[768]);
            }
            
            questions.add(q);
        }

        // Persist generated questions with session reference
        for (Question q : questions) {
            q.setSession(session);
            questionRepository.save(q);
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

            // Count answered questions for this session
            int answeredCount = answerRepository.findBySessionId(sessionId).size();

            ObjectNode resumeInfo = JsonMapper.builder().build().createObjectNode();
            resumeInfo.put("sessionId", sessionId);
            resumeInfo.put("categoryId", category);
            resumeInfo.put("difficulty", session.getDifficulty());
            resumeInfo.put("answeredQuestions", answeredCount);

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
            // Fetch questions for this session
            logger.info("Fetching questions for sessionId={}, userId={}", session.getId(), userId);
            
            // Diagnostic: Count rows without loading vector embedding column to isolate pgvector issue
            long questionCount = questionRepository.countBySessionId(session.getId());
            logger.info("Diagnostic count query returned {} questions for sessionId={} (without vector_embedding)",
                questionCount, session.getId());
            
            // Diagnostic: Check if any rows exist in the database directly (excluding vector column)
            List<Object[]> nativeResult = questionRepository.findBySessionIdWithoutVector(session.getId());
            logger.info("Native query (no vector_embedding) returned {} questions for sessionId={}",
                nativeResult.size(), session.getId());
            
            // Diagnostic: Check if any rows exist in the database directly (with vector column - this may fail)
            List<Question> rawQuestions = questionRepository.findBySessionId(session.getId());
            logger.info("Raw query returned {} questions for sessionId={}", rawQuestions.size(), session.getId());
            
            List<Question> questions = rawQuestions;
            if (questions.isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            JsonMapper mapper = JsonMapper.builder().build();
            ArrayNode questionsArray = mapper.createArrayNode();
            for (Question q : questions) {
                ObjectNode qNode = mapper.createObjectNode();
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
    public ResponseEntity<ResultsResponse> finishInterview(
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

        // Batch evaluate all answers and persist evaluations
        java.util.List<Answer> answers = answerRepository.findBySessionId(session.getId());
        double totalScore = 0;
        int evaluatedCount = 0;
        java.util.List<EvaluationDetail> evaluations = new java.util.ArrayList<>();

        for (Answer answer : answers) {
            try {
                Evaluation evaluation;
                if (answer.getQuestion().getType() == QuestionType.CODE) {
                    evaluation = aiGatewayService.evaluateCodingAnswer(answer.getQuestion(), answer);
                } else {
                    evaluation = aiGatewayService.evaluateTheoryAnswer(answer.getQuestion(), answer);
                }

                // Persist evaluation to database so results survive page reloads
                evaluationRepository.save(evaluation);

                // Parse strengths and weaknesses from JSONB strings into lists
                JsonMapper mapper = JsonMapper.builder().build();
                ArrayNode strengthsArray = (ArrayNode) mapper.readTree(evaluation.getStrengthsJson());
                ArrayNode weaknessesArray = (ArrayNode) mapper.readTree(evaluation.getWeaknessesJson());

                EvaluationDetail detail = new EvaluationDetail();
                detail.setEvaluationId(evaluation.getId());
                detail.setQuestionType(answer.getQuestion().getType());
                detail.setQuestionText(answer.getQuestion().getQuestionText());
                detail.setScore(evaluation.getScore());
                detail.setStatus(evaluation.getStatus());
                detail.setIsCorrect(evaluation.getIsCorrect());
                detail.setCorrectnessExplanation(evaluation.getCorrectnessExplanation());
                detail.setTimeComplexity(evaluation.getTimeComplexity());
                detail.setSpaceComplexity(evaluation.getSpaceComplexity());

                // Convert JSONB arrays to lists
                java.util.List<String> strengths = new java.util.ArrayList<>();
                for (JsonNode sn : strengthsArray) {
                    strengths.add(sn.asText());
                }
                detail.setStrengths(strengths);

                java.util.List<String> weaknesses = new java.util.ArrayList<>();
                for (JsonNode wn : weaknessesArray) {
                    weaknesses.add(wn.asText());
                }
                detail.setWeaknesses(weaknesses);

                detail.setImprovedAnswer(evaluation.getImprovedAnswer());
                detail.setUserAnswerText(answer.getAnswerText());
                if (answer.getQuestion().getType() == QuestionType.CODE) {
                    detail.setSubmittedCode(answer.getAnswerText()); // Code is also in answerText for coding questions
                }

                // Resolve topic names from categoryId for display
                detail.setTopicNames(resolveTopicNamesFromCategoryId(answer.getQuestion().getCategoryId()));

                evaluations.add(detail);
                totalScore += evaluation.getScore();
                evaluatedCount++;

            } catch (Exception e) {
                EvaluationDetail failedEval = new EvaluationDetail();
                failedEval.setQuestionType(answer.getQuestion().getType());
                failedEval.setQuestionText(answer.getQuestion().getQuestionText());
                failedEval.setStatus(EvaluationStatus.FAILED);
                evaluations.add(failedEval);
            }
        }

        // Calculate overall score as weighted average
        double overallScore = evaluatedCount > 0 ? totalScore / evaluatedCount : 0;

        ResultsResponse response = new ResultsResponse();
        response.setSessionId(session.getId());
        response.setStatus(SessionStatus.COMPLETED);
        response.setOverallScore(Math.round(overallScore * 100.0) / 100.0); // Round to 2 decimal places
        response.setEvaluations(evaluations);

        return ResponseEntity.ok(response);
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
            // Find all evaluations for this session's questions
            java.util.List<Evaluation> evaluationsList = evaluationRepository.findByAnswerSessionIdOrderByScoreDesc(session.getId());

            double totalScore = 0;
            int evaluatedCount = 0;
            java.util.List<EvaluationDetail> evaluations = new java.util.ArrayList<>();

            for (Evaluation eval : evaluationsList) {
                if (eval == null || eval.getAnswer() == null) continue;

                Question question = eval.getAnswer().getQuestion();
                Answer answer = eval.getAnswer();

                JsonMapper mapper = JsonMapper.builder().build();
                ArrayNode strengthsArray = (ArrayNode) mapper.readTree(eval.getStrengthsJson());
                ArrayNode weaknessesArray = (ArrayNode) mapper.readTree(eval.getWeaknessesJson());

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

                java.util.List<String> strengths = new java.util.ArrayList<>();
                for (JsonNode sn : strengthsArray) {
                    strengths.add(sn.asText());
                }
                detail.setStrengths(strengths);

                java.util.List<String> weaknesses = new java.util.ArrayList<>();
                for (JsonNode wn : weaknessesArray) {
                    weaknesses.add(wn.asText());
                }
                detail.setWeaknesses(weaknesses);

                detail.setImprovedAnswer(eval.getImprovedAnswer());
                detail.setUserAnswerText(answer.getAnswerText());
                if (answer.getQuestion().getType() == QuestionType.CODE) {
                    detail.setSubmittedCode(answer.getAnswerText());
                }

                // Resolve topic names from categoryId for display
                detail.setTopicNames(resolveTopicNamesFromCategoryId(question.getCategoryId()));

                evaluations.add(detail);
                totalScore += eval.getScore();
                evaluatedCount++;
            }

            double overallScore = evaluatedCount > 0 ? totalScore / evaluatedCount : 0;

            ResultsResponse response = new ResultsResponse();
            response.setSessionId(session.getId());
            response.setStatus(status);
            response.setOverallScore(Math.round(overallScore * 100.0) / 100.0);
            response.setEvaluations(evaluations);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch results"));
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
