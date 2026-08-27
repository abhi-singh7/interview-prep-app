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
        // Total completed sessions (the average score is derived below, once the per-session
        // marks are available — same rule as My Sessions and Score History).
        Long totalSessions = entityManager.createQuery(
                "SELECT COUNT(s) FROM InterviewSession s WHERE s.user.id = :userId AND s.status = 'COMPLETED'",
                Long.class).setParameter("userId", userId).getSingleResult();

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

        // FIX-03 / FIX-07: one row per completed session, with the category resolved to a
        // human-readable name instead of a raw id.
        // Score unification (same marks as My Sessions badge): total points earned = SUM of
        // SUCCESS scores + question count, so the frontend renders "earned / possible" identically.
        List<Object[]> sessionRows = entityManager.createNativeQuery(
                "WITH session_marks AS (" +
                "    SELECT s.id as session_id, s.ended_at, s.difficulty, SUM(e.score)::int as total_earned, qcnt.q_count as question_count " +
                "    FROM interview_sessions s " +
                "    JOIN answers a ON a.session_id = s.id " +
                "    JOIN evaluations e ON e.answer_id = a.id AND e.status = 'SUCCESS' AND e.score IS NOT NULL " +
                "    LEFT JOIN (SELECT session_id, COUNT(*) AS q_count FROM questions GROUP BY session_id) qcnt ON qcnt.session_id = s.id " +
                "    WHERE s.user_id = :userId AND s.status = 'COMPLETED' " +
                "    GROUP BY s.id, s.ended_at, s.difficulty, qcnt.q_count" +
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
                "SELECT sm.session_id, sm.ended_at, sm.difficulty, sm.total_earned, sm.question_count, sc.category_name " +
                "FROM session_marks sm " +
                 "LEFT JOIN session_cat sc ON sc.session_id = sm.session_id " +
                 "ORDER BY sm.ended_at DESC")
                .setParameter("userId", userId)
                .getResultList();

        List<SessionSummary> sessions = sessionRows.stream().map(row -> {
            SessionSummary summary = new SessionSummary();
            summary.setSessionId(((Number) row[0]).longValue());
            summary.setEndedAt(toLocalDateTime(row[1]));
            Object difficultyObj = row[2];
            summary.setDifficulty(difficultyObj != null ? difficultyObj.toString() : "N/A");
            Object scoreObj = row[3];
            // Score History now carries total points earned (same value as the My Sessions badge),
            // and questionCount lets the frontend render it as "earned / possible" (e.g. 9/100).
            summary.setScore(scoreObj != null ? ((Number) scoreObj).doubleValue() : 0.0);
            Object qCountObj = row[4];
            summary.setQuestionCount(qCountObj != null ? ((Number) qCountObj).intValue() : 0);
            Object categoryObj = row[5];
            summary.setCategoryId(categoryObj != null ? (String) categoryObj : "N/A");
            return summary;
        }).toList();

        // Average Score (Option B — same rule as My Sessions and the table below): mean of
        // each session's "earned / possible" ratio, unanswered questions counting as zero,
        // on a 0-10 scale. Null when no completed sessions were evaluated.
        double ratioSum = 0.0;
        int ratioCount = 0;
        for (SessionSummary s : sessions) {
            if (s.getQuestionCount() != null && s.getQuestionCount() > 0 && s.getScore() != null) {
                ratioSum += s.getScore() / ((double) s.getQuestionCount() * 10.0);
                ratioCount++;
            }
        }

        AnalyticsResponse response = new AnalyticsResponse();
        response.setTotalSessions(totalSessions != null ? totalSessions.intValue() : 0);
        if (ratioCount > 0) {
            response.setAvgScore(Math.round((ratioSum / ratioCount) * 10.0 * 100.0) / 100.0);
        }
        response.setCategoryBreakdown(categoryMapperToMap(categoryBreakdown));
        response.setSessions(sessions);

        return response;
    }

    // Native queries can hand back Postgres TIMESTAMP as either a LocalDateTime or a raw
    // java.sql.Timestamp depending on the Hibernate/JDBC driver path; normalize both.
    private static java.time.LocalDateTime toLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof java.time.LocalDateTime ldt) return ldt;
        if (value instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        if (value instanceof java.util.Date d) {
            return new java.sql.Timestamp(d.getTime()).toLocalDateTime();
        }
        throw new IllegalStateException("Unexpected type for ended_at: " + value.getClass().getName());
    }

    private Map<String, Double> categoryMapperToMap(ObjectNode node) {
        java.util.Map<String, Double> result = new java.util.LinkedHashMap<>();
        node.forEachEntry((key, value) -> {
            result.put(key, value.asDouble());
        });
        return result;
    }
}
