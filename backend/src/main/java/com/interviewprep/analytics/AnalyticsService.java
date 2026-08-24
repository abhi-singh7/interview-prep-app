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

        Double avgScore = entityManager.createQuery(
                "SELECT AVG(e.score) FROM Evaluation e JOIN e.answer a JOIN a.session s " +
                "WHERE s.user.id = :userId AND s.status = 'COMPLETED' AND e.score IS NOT NULL",
                Double.class).setParameter("userId", userId).getSingleResult();

        double avgScoreVal = avgScore != null ? Math.round(avgScore * 100.0) / 100.0 : 0;

        // FIX-06: split the comma/brace separated category_id list so multi-topic questions
        // contribute to every category they belong to (the old join only matched single IDs).
        List<Map<String, Object>> categoryBreakdownRows = entityManager.createNativeQuery(
                "SELECT c.name as category_name, AVG(e.score)::float as avg_score FROM evaluations e " +
                "JOIN answers a ON e.answer_id = a.id " +
                "JOIN questions q ON a.question_id = q.id " +
                "JOIN interview_sessions s ON q.session_id = s.id AND s.user_id = :userId " +
                "JOIN categories c ON c.id::text = ANY(string_to_array(regexp_replace(trim(q.category_id), '[{}]', ','), ',')) " +
                "WHERE s.status = 'COMPLETED' AND e.score IS NOT NULL " +
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

        // FIX-03 / FIX-07: one row per completed session, averaged over ALL evaluations
        // (code + theory), with the category resolved to a human-readable name instead of a raw id.
        List<Object[]> sessionRows = entityManager.createNativeQuery(
                "WITH session_avg AS (" +
                "    SELECT s.id as session_id, s.ended_at, s.difficulty, AVG(e.score)::float as avg_score " +
                "    FROM interview_sessions s " +
                "    JOIN answers a ON a.session_id = s.id " +
                "    JOIN evaluations e ON e.answer_id = a.id AND e.score IS NOT NULL " +
                "    WHERE s.user_id = :userId AND s.status = 'COMPLETED' " +
                "    GROUP BY s.id, s.ended_at, s.difficulty" +
                "), " +
                "session_cat AS (" +
                "    SELECT DISTINCT ON (s.id) s.id as session_id, c.name as category_name " +
                "    FROM interview_sessions s " +
                "    JOIN answers a ON a.session_id = s.id " +
                "    JOIN evaluations e ON e.answer_id = a.id AND e.score IS NOT NULL " +
                "    JOIN questions q ON q.id = a.question_id " +
                "    LEFT JOIN categories c ON c.id::text = ANY(string_to_array(regexp_replace(trim(q.category_id), '[{}]', ','), ',')) " +
                "    WHERE s.user_id = :userId AND s.status = 'COMPLETED' AND c.name IS NOT NULL AND c.name <> '' " +
                "    ORDER BY s.id, c.name" +
                ") " +
                "SELECT sa.session_id, sa.ended_at, sa.difficulty, sa.avg_score, sc.category_name " +
                "FROM session_avg sa " +
                "LEFT JOIN session_cat sc ON sc.session_id = sa.session_id " +
                "ORDER BY sa.ended_at DESC")
                .setParameter("userId", userId)
                .unwrap(NativeQuery.class)
                .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
                .getResultList();

        List<SessionSummary> sessions = sessionRows.stream().map(row -> {
            SessionSummary summary = new SessionSummary();
            summary.setSessionId(((Number) row[0]).longValue());
            summary.setEndedAt((java.time.LocalDateTime) row[1]);
            Object difficultyObj = row[2];
            summary.setDifficulty(difficultyObj != null ? difficultyObj.toString() : "N/A");
            Object scoreObj = row[3];
            double scoreVal = scoreObj != null ? ((Number) scoreObj).doubleValue() : 0.0;
            // FIX-02: round to 2 decimals (was previously *100 without dividing, e.g. "745/10")
            summary.setScore(Math.round(scoreVal * 100.0) / 100.0);
            Object categoryObj = row[4];
            summary.setCategoryId(categoryObj != null ? (String) categoryObj : "N/A");
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
