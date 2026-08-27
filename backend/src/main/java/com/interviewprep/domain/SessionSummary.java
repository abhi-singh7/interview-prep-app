package com.interviewprep.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SessionSummary {
    private Long sessionId;
    private LocalDateTime endedAt;
    private String categoryId;
    private String difficulty;
    // Total points earned in this session (SUM of SUCCESS evaluation scores — same value as the
    // My Sessions badge); frontend renders it as "earned / possible" using questionCount.
    private Double score;
    // Total questions in the session → possible max = questionCount * 10
    private Integer questionCount;
}
