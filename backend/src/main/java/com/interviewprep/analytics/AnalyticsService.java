package com.interviewprep.analytics;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import com.interviewprep.domain.*;
import jakarta.persistence.EntityManager;
import org.hibernate.query.NativeQuery;
import org.hibernate.transform.Transformers;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private final EntityManager entityManager;

    public AnalyticsService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public AnalyticsResponse getPerformanceData(Long userId) {
        // Calculate total sessions and average score
        Long totalSessions = entityManager.createQuery(
                "SELECT COUNT(s) FROM InterviewSession s WHERE s.user.id = :userId AND s.status = 'COMPLETED'",
                Long.class).setParameter("userId", userId).getSingleResult();

        // Average score: per-session percentage (earned / possible), averaged across sessions.
        Number avgScoreNum = (Number) entityManager.createNativeQuery(
                "SELECT AVG(sa.pct)::float FROM (" +
                "  SELECT ROUND((SUM(e.score) * 100.0) / NULLIF(qc.total_questions * 10, 0), 2) AS pct " +
                "  FROM interview_sessions s " +
                "  JOIN questions q ON q.session_id = s.id " +
                "  JOIN answers a ON a.question_id = q.id AND a.answer_text IS NOT NULL " +
                "  JOIN evaluations e ON e.answer_id = a.id " +
                "  JOIN (" +
                "    SELECT q2.session_id, COUNT(*) AS total_questions FROM questions q2 GROUP BY q2.session_id" +
                "  ) qc ON qc.session_id = s.id " +
                "  WHERE s.user_id = :userId AND s.status = 'COMPLETED' " +
                "  GROUP BY s.id, qc.total_questions" +
                ") sa")
                .setParameter("userId", userId)
                .getSingleResult();

        Double avgScore = avgScoreNum != null ? avgScoreNum.doubleValue() : null;

        double avgScoreVal = avgScore != null ? Math.round(avgScore * 100.0) / 100.0 : 0;

        // Category breakdown: only the latest evaluation per answer, using string_to_array + EXISTS so comma-separated category_id values match correctly.
        // Security: The userId parameter is bound via setParameter(), preventing SQL injection.
        // All user-supplied data flows through JPA parameterized queries; no string interpolation occurs.
        List<Map<String, Object>> categoryBreakdownRows = entityManager.createNativeQuery(
                "SELECT c.name as category_name, AVG(latest_e.score)::float as avg_score FROM (" +
                "  SELECT DISTINCT ON (a3.id) e3.id AS eval_id, e3.score, a3.id AS ans_id " +
                "  FROM evaluations e3 " +
                "  JOIN answers a3 ON e3.answer_id = a3.id " +
                "  ORDER BY a3.id, e3.id DESC" +
                ") latest_e " +
                "JOIN answers a4 ON a4.id = latest_e.ans_id " +
                "JOIN questions q ON a4.question_id = q.id " +
                "JOIN interview_sessions s ON q.session_id = s.id AND s.user_id = :userId " +
                "LEFT JOIN categories c ON EXISTS (" +
                "  SELECT 1 FROM unnest(string_to_array(nullif(trim(q.category_id), ''), ',')::bigint[]) AS x(id) WHERE x.id = c.id" +
                ") " +
                "WHERE s.status = 'COMPLETED' AND latest_e.score IS NOT NULL " +
                "GROUP BY c.name ORDER BY avg_score DESC")
                .setParameter("userId", userId)
                .unwrap(NativeQuery.class)
                .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
                .getResultList();

        JsonMapper mapper = JsonMapper.builder().build();
        ObjectNode categoryBreakdown = mapper.createObjectNode();
        for (Map<String, Object> row : categoryBreakdownRows) {
            String categoryName = (String) row.get("category_name");
            if (categoryName == null || categoryName.isBlank()) continue;
            Number scoreNum = (Number) row.get("avg_score");
            double scoreVal = scoreNum != null ? scoreNum.doubleValue() : 0.0;
            categoryBreakdown.put(categoryName, Math.round(scoreVal * 100.0) / 100.0);
        }

        // Query completed sessions ordered by endedAt descending (most recent first).
        // - All question types included (removed q.type = 'CODE' filter so THEORY questions contribute to session averages).
        // - Multi-category resolution via string_to_array + unnest + EXISTS join with categories table.
        // - Only the latest evaluation per answer contributes to AVG(e.score) via DISTINCT ON subquery.
        // - answeredCount tracks distinct evaluated answers; avg_score is NULL when no evaluations exist (no answers).
        List<Object[]> sessionRows = entityManager.createNativeQuery(
                "SELECT s.id as session_id, s.ended_at, " +
                "COALESCE(string_agg(DISTINCT c.name, ',' ORDER BY c.name), 'N/A') as category_name, " +
                "s.difficulty, AVG(latest_e.score)::float as avg_score, " +
                "(SELECT COUNT(*) FROM questions q2 WHERE q2.session_id = s.id) as question_count, " +
                "COUNT(DISTINCT latest_e.ans_id) as answered_count " +
                "FROM interview_sessions s " +
                "LEFT JOIN questions q ON q.session_id = s.id " +
                "LEFT JOIN categories c ON EXISTS (" +
                "  SELECT 1 FROM unnest(string_to_array(nullif(trim(q.category_id), ''), ',')::bigint[]) AS x(id) WHERE x.id = c.id" +
                ") " +
                "LEFT JOIN answers a ON a.question_id = q.id AND a.answer_text IS NOT NULL " +
                "LEFT JOIN (" +
                "  SELECT DISTINCT ON (a5.id) e5.id, e5.score, a5.id AS ans_id " +
                "  FROM evaluations e5 " +
                "  JOIN answers a5 ON e5.answer_id = a5.id " +
                "  ORDER BY a5.id, e5.id DESC" +
                ") latest_e ON latest_e.ans_id = a.id " +
                "WHERE s.user_id = :userId AND s.status = 'COMPLETED' " +
                "GROUP BY s.id, s.ended_at, s.difficulty " +
                "ORDER BY s.ended_at DESC").setParameter("userId", userId).getResultList();

        List<SessionSummary> sessions = sessionRows.stream().map(row -> {
            SessionSummary summary = new SessionSummary();
            summary.setSessionId(((Number) row[0]).longValue());
            summary.setEndedAt((java.time.LocalDateTime) row[1]);
            Object categoryObj = row[2];

            summary.setCategoryName(categoryObj != null ? (String) categoryObj : "N/A");
            summary.setDifficulty(row[3] != null ? (String) row[3] : "N/A");
            Object scoreObj = row[4];
            double scoreVal = scoreObj != null ? ((Number) scoreObj).doubleValue() : 0.0;
            int answeredCount = row[6] instanceof Number n ? n.intValue() : 0;

            // Total score for display: avg_score * answered_count (only count answered questions, not all).
            // If no answers were evaluated (answeredCount == 0), total is 0.
            double totalScore = answeredCount > 0 && row[4] != null ? Math.round(scoreVal * answeredCount) : 0;
            summary.setScore((int) totalScore);

            Object questionCountObj = row[5];
            int countVal = questionCountObj instanceof Number n ? n.intValue() : 0;
            summary.setQuestionCount(countVal);
            summary.setAnsweredCount(answeredCount);
            return summary;
        }).toList();

        AnalyticsResponse response = new AnalyticsResponse();
        response.setTotalSessions(totalSessions != null ? totalSessions.intValue() : 0);
        response.setAvgScore(avgScoreVal);
        response.setCategoryBreakdown(categoryMapperToMap(categoryBreakdown));
        response.setSessions(sessions);

        return response;
    }

    private Map<String, Double> categoryMapperToMap(ObjectNode node) {
        java.util.Map<String, Double> result = new java.util.LinkedHashMap<>();
        node.forEachEntry((key, value) -> {
            result.put(key, value.asDouble());
        });
        return result;
    }
}
