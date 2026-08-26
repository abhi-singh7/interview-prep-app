package com.interviewprep.domain;

import lombok.Data;
import java.util.List;

@Data
public class ResultsResponse {
    private Long sessionId;
    private SessionStatus status;
    private double overallScore;
    private List<EvaluationDetail> evaluations;

    // Every question in the session with the user's answer and the reference (correct) answer,
    // so unanswered/wrong questions remain reviewable after finishing
    private List<QuestionReviewDetail> questions;
}
