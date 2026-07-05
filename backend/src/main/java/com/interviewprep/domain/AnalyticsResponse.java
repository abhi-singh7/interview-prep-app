package com.interviewprep.domain;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * DTO representing aggregated analytics data for a user's interview preparation progress.
 * <p>
 * This response object is returned by the {@link com.interviewprep.analytics.AnalyticsController}
 * and contains:
 * <ul>
 *   <li>Total number of completed interview sessions</li>
 *   <li>Average score across all sessions (0-100 scale)</li>
 *   <li>Performance breakdown by category (e.g., Data Structures, Algorithms) as score averages</li>
 *   <li>Detailed summary of each session including scores and question counts</li>
 * </ul>
 * <p>
 * The {@code categoryBreakdown} map uses category names as keys and average scores as values,
 * enabling the frontend to display performance trends across different interview topics.
 * </p>
 *
 * @see SessionSummary
 */
@Data
public class AnalyticsResponse {
    /** Total number of interview sessions completed by the user. */
    private Integer totalSessions;

    /** Average score (0-100) across all completed sessions. Null if no sessions exist. */
    private Double avgScore;

    /** Map of category names to their average scores, providing per-topic performance insights. */
    private Map<String, Double> categoryBreakdown;

    /** List of individual session summaries with detailed scoring information. */
    private List<SessionSummary> sessions;
}
