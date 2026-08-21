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

        // Calculate category breakdown using native query for JSONB operations
        List<Map<String, Object>> categoryBreakdownRows = entityManager.createNativeQuery(
                "SELECT c.name as category_name, AVG(e.score)::float as avg_score FROM evaluations e " +
                "JOIN answers a ON e.answer_id = a.id " +
                "JOIN questions q ON a.question_id = q.id " +
                "JOIN interview_sessions s ON q.session_id = s.id AND s.user_id = :userId " +
                "LEFT JOIN categories c ON q.category_id::text = c.id::text " +
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

        // Query completed sessions ordered by endedAt descending (most recent first)
        List<Object[]> sessionRows = entityManager.createNativeQuery(
                "SELECT s.id as session_id, s.ended_at, q.category_id::text as category_name, " +
                "s.difficulty, AVG(e.score)::float as avg_score FROM interview_sessions s " +
                "LEFT JOIN questions q ON q.session_id = s.id AND q.type = 'CODE' " +
                "LEFT JOIN answers a ON a.question_id = q.id AND a.answer_text IS NOT NULL " +
                "LEFT JOIN evaluations e ON e.answer_id = a.id " +
                "WHERE s.user_id = :userId AND s.status = 'COMPLETED' " +
                "GROUP BY s.id, s.ended_at, q.category_id, s.difficulty " +
                "ORDER BY s.ended_at DESC").setParameter("userId", userId).getResultList();

        List<SessionSummary> sessions = sessionRows.stream().map(row -> {
            SessionSummary summary = new SessionSummary();
            summary.setSessionId(((Number) row[0]).longValue());
            summary.setEndedAt((java.time.LocalDateTime) row[1]);
            Object categoryObj = row[2];
            summary.setCategoryId(categoryObj != null ? (String) categoryObj : "N/A");
            summary.setDifficulty(row[3] != null ? (String) row[3] : "N/A");
            Object scoreObj = row[4];
            double scoreVal = scoreObj != null ? ((Number) scoreObj).doubleValue() : 0.0;
            summary.setScore((int) Math.round(scoreVal * 100.0));
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
