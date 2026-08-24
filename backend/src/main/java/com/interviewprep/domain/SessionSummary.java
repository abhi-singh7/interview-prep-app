package com.interviewprep.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SessionSummary {
    private Long sessionId;
    private LocalDateTime endedAt;
    private String categoryId;
    private String difficulty;
    // FIX-02: Double so scores keep 2-decimal precision (e.g. 7.45) instead of the old x100 bug
    private Double score;
}
