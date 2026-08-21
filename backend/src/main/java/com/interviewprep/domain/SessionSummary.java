package com.interviewprep.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SessionSummary {
    private Long sessionId;
    private LocalDateTime endedAt;
    private String categoryId;
    private String difficulty;
    private Integer score;
}
