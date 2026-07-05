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

/**
 * Service class for processing and retrieving user analytics data.
 * It performs complex queries to aggregate performance metrics, category breakdowns,
 * and session summaries from the database.
 */
@Service
public class AnalyticsService {

    private final EntityManager entityManager;

    /**
     * Constructs the AnalyticsService with the required EntityManager.
     *
     * @param entityManager The JPA EntityManager for executing database queries.
     */
    public AnalyticsService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Retrieves a comprehensive analytics response for a specific user.
     * This includes total completed sessions, average score across all sessions,
     * a breakdown of scores by category, and a list of recent session summaries.
     *
     * @param userId The unique identifier of the user.
     * @return An AnalyticsResponse object containing all aggregated metrics.
     */
    public AnalyticsResponse getPerformanceData(Long userId) {
        // Calculate total number of completed sessions for the user.
        Long totalSessions = entityManager.createQuery(
                "SELECT COUNT(s) FROM InterviewSession s WHERE s.user.id = :userId AND s.status = 'COMPLETED'",
                Long.class).setParameter("userId", userId).getSingleResult();

        /**
         * Calculate the average score across all completed sessions.
         * The score is calculated as a percentage (earned / possible) per session,
         * then averaged across all sessions where the user has completed interviews.
         */
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

        // Round the average score to 2 decimal places.
        double avgScoreVal = avgScore != null ? Math.round(avgScore * 100.0) / 100.0 : 0;

        /**
         * Retrieve a breakdown of average scores by category.
         * This query identifies the latest evaluation for each answer and joins it with
         * categories. It handles multi-category assignments by unnesting the category_id array.
         *
         * Security Note: Uses parameterized queries to prevent SQL injection.
         */
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

        /**
         * Retrieve a list of recent completed sessions with summary details.
         * Includes category names (aggregated), difficulty, average scores,
         * and counts for questions and answered questions.
         *
         * - Uses DISTINCT ON to ensure only the most recent evaluation per answer is considered.
         * - Aggregates category names into a comma-separated string.
         */
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

            // Calculate total score for display: avg_score * answered_count (only count answered questions, not all).
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

    /**
     * Converts a Jackson ObjectNode containing category scores into a LinkedHashMap.
     *
     * @param node The ObjectNode containing category names as keys and scores as values.
     * @return A LinkedHashMap of category names to their respective scores.
     */
    private Map<String, Double> categoryMapperToMap(ObjectNode node) {
        java.util.Map<String, Double> result = new java.util.LinkedHashMap<>();
        node.forEachEntry((key, value) -> {
            result.put(key, value.asDouble());
        });
        return result;
    }
}
