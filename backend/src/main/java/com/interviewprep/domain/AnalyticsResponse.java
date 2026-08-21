package com.interviewprep.domain;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AnalyticsResponse {
    private Integer totalSessions;
    private Double avgScore;
    private Map<String, Double> categoryBreakdown;
    private List<SessionSummary> sessions;
}
