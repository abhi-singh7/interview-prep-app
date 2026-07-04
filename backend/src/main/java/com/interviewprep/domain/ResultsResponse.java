package com.interviewprep.domain;

import lombok.Data;
import java.util.List;

@Data
public class ResultsResponse {
    private Long sessionId;
    private SessionStatus status;
    private double overallScore;
    private List<EvaluationDetail> evaluations;
    private String languageId;
    private String languageName;
    private List<String> topicNames;
    private int answeredCount;
    private int totalCount;
    private Integer totalEarned;
}
