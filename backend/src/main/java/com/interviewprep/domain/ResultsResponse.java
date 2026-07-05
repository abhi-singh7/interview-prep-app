package com.interviewprep.domain;

import lombok.Data;
import java.util.List;

/**
 * DTO representing the complete results of a finished interview session.
 * <p>
 * This object is returned by the {@code InterviewController} after an interview session has been completed,
 * aggregating all per-question evaluations into a single response that the frontend can render as a summary page.
 * It includes both high-level metrics (overall score, answered/total counts) and detailed per-question feedback.
 * </p>
 * <h3>Content:</h3>
 * <ul>
 *   <li><b>Session metadata:</b> session ID, status, language, topic names.</li>
 *   <li><b>Aggregate scores:</b> overall score and total points earned.</li>
 *   <li><b>Per-question evaluations:</b> detailed feedback for each answered question via {@link EvaluationDetail}.</li>
 * </ul>
 * 
 * @see InterviewSession
 * @see EvaluationDetail
 */
@Data
public class ResultsResponse {

    /** The unique identifier of the completed interview session. */
    private Long sessionId;

    /** The final status of this session (e.g., COMPLETED). */
    private SessionStatus status;

    /** Overall score across all evaluated answers, averaged from individual question scores. */
    private double overallScore;

    /** Detailed per-question evaluations including scores, feedback, and improvements. */
    private List<EvaluationDetail> evaluations;

    /** The programming language ID configured for this session (e.g., "java", "python"). */
    private String languageId;

    /** Human-readable name of the programming language (e.g., "Java", "Python"). */
    private String languageName;

    /** List of human-readable topic names that were covered in this interview. */
    private List<String> topicNames;

    /** Number of questions the user actually submitted answers for. */
    private int answeredCount;

    /** Total number of questions generated for this session. */
    private int totalCount;

    /** Total points earned across all evaluated answers (sum of individual scores). */
    private Integer totalEarned;
}
