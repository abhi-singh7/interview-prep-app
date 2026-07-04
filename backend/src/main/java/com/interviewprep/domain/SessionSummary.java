package com.interviewprep.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SessionSummary {
    private Long sessionId;
    private LocalDateTime endedAt;
    private String categoryName;
    private String difficulty;
    private Integer score;
    private Integer questionCount;
    private Integer answeredCount;
}
