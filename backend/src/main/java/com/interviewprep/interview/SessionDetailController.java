package com.interviewprep.interview;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewprep.config.AuthenticationContext;
import com.interviewprep.domain.*;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller exposing detailed view of an interview session (questions + answers + evaluations).
 * Enforces ownership checks and returns 404 for ABANDONED sessions.
 */
@RestController
@RequestMapping("/interview/session")
public class SessionDetailController {

    private final InterviewSessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final EvaluationRepository evaluationRepository;
    private final CategoryRepository categoryRepository;
    private final AuthenticationContext auth;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SessionDetailController(InterviewSessionRepository sessionRepository,
                                   QuestionRepository questionRepository,
                                   AnswerRepository answerRepository,
                                   EvaluationRepository evaluationRepository,
                                   CategoryRepository categoryRepository,
                                   AuthenticationContext auth) {
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.evaluationRepository = evaluationRepository;
        this.categoryRepository = categoryRepository;
        this.auth = auth;
    }

    @Transactional(readOnly = true)
    @GetMapping("/{sessionId}/detail")
    public ResponseEntity<?> getDetail(@PathVariable Long sessionId) {
        Long userId = auth.getCurrentUserId();

        InterviewSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        SessionStatus status = session.getStatus();
        if (status == SessionStatus.ABANDONED) {
            return ResponseEntity.notFound().build();
        }

        InterviewSession finalSession = session;
        if (!session.getUser().getId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden: you do not own this session"));
        }

        String languageName = resolveLanguageName(session.getLanguageId());
        List<String> topicNames = resolveTopicIdsFromJson(session.getTopicIdsJson());

        List<Question> questions = questionRepository.findBySessionId(finalSession.getId());

        // Include orphaned questions (no session_id backfill, no answers) so the list is complete.
        List<Question> orphanedQuestions = new ArrayList<>();
        if (finalSession.getCategoryId() != null && !finalSession.getCategoryId().isBlank()) {
            orphanedQuestions.addAll(questionRepository.findOrphansByCategory(finalSession.getId().toString()));
        }

        Map<Long, Answer> answersByQuestionId = new HashMap<>();
        for (Answer a : answerRepository.findBySessionId(finalSession.getId())) {
            if (a.getQuestion() != null) {
                answersByQuestionId.put(a.getQuestion().getId(), a);
            }
        }

        Map<Long, Evaluation> evaluationsByAnswerId = new HashMap<>();
        for (Evaluation ev : evaluationRepository.findByAnswerSessionIdOrderByScoreDesc(finalSession.getId())) {
            if (ev.getAnswer() != null) {
                evaluationsByAnswerId.put(ev.getAnswer().getId(), ev);
            }
        }

        List<Evaluation> allEvals = new ArrayList<>(evaluationsByAnswerId.values());
        double totalScore = 0;
        int evaluatedCount = 0;
        for (Evaluation e : allEvals) {
            if (e.getStatus() == EvaluationStatus.SUCCESS && e.getScore() != null) {
                totalScore += e.getScore();
                evaluatedCount++;
            }
        }

        InterviewSessionDetailResponse response = new InterviewSessionDetailResponse();
        response.setSessionId(finalSession.getId());
        response.setStatus(status.name());
        response.setLanguageId(session.getLanguageId());
        response.setLanguageName(languageName);
        response.setTopicNames(topicNames);
        response.setCategoryId(session.getCategoryId());
        response.setDifficulty(session.getDifficulty());
        response.setStartedAt(session.getStartedAt());
        response.setEndedAt(session.getEndedAt());

        if (evaluatedCount > 0) {
            response.setOverallScore(Math.round((totalScore / evaluatedCount) * 100.0) / 100.0);
            // totalEarned: sum of all evaluated answer scores — displayed as "earned / possible" format
            response.setTotalEarned((int) Math.round(totalScore));
        } else {
            response.setOverallScore(null);
        }

        List<InterviewSessionDetailResponse.SessionQuestionResponse> questionResponses = new ArrayList<>();
        for (Question q : questions) {
            InterviewSessionDetailResponse.SessionQuestionResponse sq = buildQuestionResponse(q, answersByQuestionId.get(q.getId()), evaluationsByAnswerId);
            questionResponses.add(sq);
        }
        // Append orphaned questions (no session_id backfill, no answer chain) as unanswered.
        for (Question q : orphanedQuestions) {
            InterviewSessionDetailResponse.SessionQuestionResponse sq = new InterviewSessionDetailResponse.SessionQuestionResponse();
            sq.setQuestionId(q.getId());
            sq.setType(q.getType() != null ? q.getType().name() : "THEORY");
            sq.setTitle(q.getTitle());
            sq.setQuestionText(q.getQuestionText());
            sq.setDescription(q.getDescription());
            sq.setCodePrompt(q.getCodePrompt());
            sq.setAnswerStatus("UNANSWERED");
            questionResponses.add(sq);
        }
        response.setQuestions(questionResponses);

        return ResponseEntity.ok(response);
    }

    private InterviewSessionDetailResponse.SessionQuestionResponse buildQuestionResponse(Question q, Answer answer, Map<Long, Evaluation> evalsByAnswerId) {
        InterviewSessionDetailResponse.SessionQuestionResponse sq = new InterviewSessionDetailResponse.SessionQuestionResponse();
        sq.setQuestionId(q.getId());
        sq.setType(q.getType() != null ? q.getType().name() : "THEORY");
        sq.setTitle(q.getTitle());
        sq.setQuestionText(q.getQuestionText());
        sq.setDescription(q.getDescription());
        sq.setCodePrompt(q.getCodePrompt());

        if (answer == null) {
            sq.setAnswerStatus("UNANSWERED");
            return sq;
        }

        InterviewSessionDetailResponse.SessionAnswerResponse sa = new InterviewSessionDetailResponse.SessionAnswerResponse();
        sa.setAnswerId(answer.getId());
        sa.setAnswerText(answer.getAnswerText());
        if (answer.getLanguageSubmitted() != null && !answer.getLanguageSubmitted().isBlank()) {
            sa.setLanguageSubmitted(answer.getLanguageSubmitted());
        }

        Evaluation eval = evalsByAnswerId.get(answer.getId());
        if (eval != null && eval.getStatus() == EvaluationStatus.SUCCESS) {
            InterviewSessionDetailResponse.SessionEvaluationResponse se = new InterviewSessionDetailResponse.SessionEvaluationResponse();
            se.setEvaluationId(eval.getId());
            se.setScore(eval.getScore());
            se.setStatus(eval.getStatus().name());
            se.setStrengths(parseJsonArrayToList(eval.getStrengthsJson()));
            se.setWeaknesses(parseJsonArrayToList(eval.getWeaknessesJson()));
            se.setImprovedAnswer(eval.getImprovedAnswer());
            se.setIsCorrect(eval.getIsCorrect());
            se.setCorrectnessExplanation(eval.getCorrectnessExplanation());
            se.setTimeComplexity(eval.getTimeComplexity());
            se.setSpaceComplexity(eval.getSpaceComplexity());
            sa.setEvaluation(se);
        } else if (eval != null && eval.getStatus() == EvaluationStatus.FAILED) {
            InterviewSessionDetailResponse.SessionEvaluationResponse se = new InterviewSessionDetailResponse.SessionEvaluationResponse();
            se.setEvaluationId(eval.getId());
            se.setScore(null);
            se.setStatus(EvaluationStatus.FAILED.name());
            sa.setEvaluation(se);
        }

        sq.setAnswerStatus("ANSWERED");
        sq.setAnswer(sa);
        return sq;
    }

    /** Returns the list of answers already submitted for a given session (used by resume sync). */
    @Transactional(readOnly = true)
    @GetMapping("/{sessionId}/answers")
    public ResponseEntity<?> getAnswersForSession(@PathVariable Long sessionId) {
        Long userId = auth.getCurrentUserId();

        InterviewSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) return ResponseEntity.notFound().build();
        if (session.getStatus() == SessionStatus.ABANDONED) return ResponseEntity.notFound().build();
        if (!session.getUser().getId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden: you do not own this session"));
        }

        List<Answer> answers = answerRepository.findBySessionId(session.getId());
        java.util.List<Map<String, Object>> result = new ArrayList<>();
        for (Answer a : answers) {
            Map<String, Object> row = new HashMap<>();
            row.put("answerId", a.getId());
            row.put("questionId", a.getQuestion() != null ? a.getQuestion().getId() : null);
            row.put("text", a.getAnswerText() != null ? a.getAnswerText() : "");
            row.put("languageSubmitted", (a.getLanguageSubmitted() != null && !a.getLanguageSubmitted().isBlank()) ? a.getLanguageSubmitted() : "");
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }

    private String resolveLanguageName(String languageIdStr) {
        if (languageIdStr == null || languageIdStr.isBlank()) return "";
        try {
            Long id = Long.parseLong(languageIdStr);
            java.util.Optional<Category> cat = categoryRepository.findById(id);
            return cat.map(Category::getName).orElse("");
        } catch (NumberFormatException e) {
            return "";
        }
    }

    private List<String> resolveTopicIdsFromJson(String topicIdsJson) {
        if (topicIdsJson == null || topicIdsJson.isBlank()) return List.of();

        String trimmed = topicIdsJson.trim();
        // Strip surrounding braces or brackets — stored as {1,2,3} or [1,2,3]
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        } else if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }

        if (trimmed.isBlank()) return List.of();

        try {
            java.util.List<Long> ids = objectMapper.readValue(trimmed, new TypeReference<List<Long>>() {});
            return categoryRepository.findByIdIn(ids).stream().map(Category::getName).toList();
        } catch (Exception e) {
            // Fallback: split on comma and parse each individually
            String[] parts = trimmed.split(",");
            List<String> names = new ArrayList<>();
            for (String part : parts) {
                try {
                    Long id = Long.parseLong(part.trim());
                    java.util.Optional<Category> cat = categoryRepository.findById(id);
                    cat.ifPresent(c -> names.add(c.getName()));
                } catch (NumberFormatException ex) {
                    // Skip unparseable entries
                }
            }
            return names;
        }
    }

    private List<String> parseJsonArrayToList(String jsonStr) {
        if (jsonStr == null || jsonStr.isBlank()) return List.of();
        try {
            java.util.List<Object> list = objectMapper.readValue(jsonStr, new TypeReference<List<Object>>() {});
            return list.stream().map(Object::toString).toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    @GetMapping("/list")
    public ResponseEntity<?> getUserSessions() {
        Long userId = auth.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        List<InterviewSession> sessions = sessionRepository.findByUserIdOrderByStartedAtDesc(userId);
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> result = new ArrayList<>();
        for (InterviewSession s : sessions) {
            Map<String, Object> row = new HashMap<>();
            if (s.getStatus() == SessionStatus.ACTIVE && s.getStartedAt() != null) {
                long hoursElapsed = java.time.Duration.between(s.getStartedAt(), now).toHours();
                int timeoutHours = s.getTimeoutHours() > 0 ? s.getTimeoutHours() : 2;
                row.put("timedOut", hoursElapsed >= timeoutHours);
            } else {
                row.put("timedOut", false);
            }
            row.put("id", s.getId());
            row.put("status", s.getStatus().name());
            row.put("languageId", s.getLanguageId() != null ? s.getLanguageId() : "");
            row.put("languageName", resolveLanguageName(s.getLanguageId()));
            row.put("difficulty", s.getDifficulty() != null ? s.getDifficulty() : "");
            row.put("topicNames", resolveTopicIdsFromJson(s.getTopicIdsJson()));
            row.put("startedAt", s.getStartedAt());
            row.put("endedAt", s.getEndedAt());
            long count = questionRepository.countBySessionId(s.getId());
            row.put("questionCount", count);
            if (s.getStatus() == SessionStatus.COMPLETED) {
                try {
                    var evals = evaluationRepository.findByAnswerSessionIdOrderByScoreDesc(s.getId());
                    double total = 0; int n = 0;
                    for (Evaluation ev : evals) {
                        if (ev != null && ev.getStatus() == EvaluationStatus.SUCCESS && ev.getScore() != null) {
                            total += ev.getScore(); n++;
                        }
                    }
                    // totalEarned: sum of all evaluated answer scores — displayed as "earned / possible" format
                    row.put("totalEarned", n > 0 ? (int) Math.round(total) : null);
                } catch (Exception e) {
                    row.put("overallScore", null);
                }
            } else {
                row.put("overallScore", null);
            }
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }

    /** Simple DTO for session list entries — used as a Map above but kept here for clarity. */
    public static class SessionListItemDTO {
        private Long id;
        private String status;
        private String languageName;
        private String difficulty;
        private List<String> topicNames = List.of();
        private LocalDateTime startedAt;
        private LocalDateTime endedAt;
        private long questionCount;
        private Double overallScore;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getLanguageName() { return languageName; }
        public void setLanguageName(String languageName) { this.languageName = languageName; }
        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
        public List<String> getTopicNames() { return topicNames; }
        public void setTopicNames(List<String> topicNames) { this.topicNames = topicNames; }
        public LocalDateTime getStartedAt() { return startedAt; }
        public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
        public LocalDateTime getEndedAt() { return endedAt; }
        public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
        public long getQuestionCount() { return questionCount; }
        public void setQuestionCount(long questionCount) { this.questionCount = questionCount; }
        public Double getOverallScore() { return overallScore; }
        public void setOverallScore(Double overallScore) { this.overallScore = overallScore; }
    }

    /** DTO for an individual session detail response. */
    @lombok.Data
    private static class SessionDetailResponseDTO {
        private Long sessionId;
        private String status;
        private String languageId;
        private String languageName;
        private List<String> topicNames = new ArrayList<>();
        private String categoryId;
        private String difficulty;
        private LocalDateTime startedAt;
        private LocalDateTime endedAt;
        private Double overallScore;
        private List<SessionQuestionResponseDTO> questions = new ArrayList<>();

        @lombok.Data
        public static class SessionQuestionResponseDTO {
            private Long questionId;
            private String type;
            private String title;
            private String questionText;
            private String description;
            private String codePrompt;
            private String answerStatus;
            private SessionAnswerDTO answer;

            @lombok.Data
            public static class SessionAnswerDTO {
                private Long answerId;
                private String answerText;
                private String languageSubmitted;
                private SessionEvaluationDTO evaluation;

                @lombok.Data
                public static class SessionEvaluationDTO {
                    private Long evaluationId;
                    private Double score;
                    private String status;
                    private List<String> strengths = new ArrayList<>();
                    private List<String> weaknesses = new ArrayList<>();
                    private String improvedAnswer;
                    private Boolean isCorrect;
                    private String correctnessExplanation;
                    private String timeComplexity;
                    private String spaceComplexity;
                }
            }
        }
    }

}
