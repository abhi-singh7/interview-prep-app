package com.interviewprep.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Type;
import java.time.LocalDateTime;

/**
 * JPA entity representing aggregated performance metrics for a user across all interview sessions.
 * <p>
 * This entity serves as a materialized view of a user's interview history, caching computed statistics
 * such as total sessions completed, average score, and per-category breakdowns. It is updated by the
 * {@code AnalyticsService} to avoid expensive real-time aggregation queries against raw session data.
 * </p>
 * <h3>Persistence Details:</h3>
 * <ul>
 *   <li><b>Table:</b> {@code user_performance}</li>
 *   <li><b>ID Strategy:</b> Auto-incrementing identity column ({@link GenerationType#IDENTITY}).</li>
 *   <li><b>User Relationship:</b> Many-to-one with {@link User} — one performance record per user.</li>
 *   <li><b>Category Breakdown:</b> Stored as a JSONB string mapping category IDs to average scores,
 *       enabling flexible analytics without schema changes for new categories.</li>
 * </ul>
 * 
 * @see User
 */
@Entity
@Table(name = "user_performance")
public class UserPerformance {

    /** Auto-generated unique identifier for this performance record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user whose performance metrics are stored here (lazy-loaded). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Total number of interview sessions completed by this user. */
    @Column(name = "total_sessions")
    private Integer totalSessions;

    /** Average score across all completed interview sessions (0-100 scale). */
    @Column(name = "avg_score")
    private Double avgScore;

    /**
     * JSONB string representing per-category average scores.
     * <p>Format: {@code {"category-id": 85.5, "another-category": 72.3}}.</p>
     * This allows flexible analytics without requiring a separate junction table for category-level metrics.
     */
    @Column(columnDefinition = "jsonb")
    private String categoryBreakdownJson;

    /** Timestamp of the last time this performance record was updated. */
    private LocalDateTime updatedAt;

    /** Default no-arg constructor required by JPA providers (e.g., Hibernate). */
    public UserPerformance() {}

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Integer getTotalSessions() { return totalSessions; }
    public void setTotalSessions(Integer totalSessions) { this.totalSessions = totalSessions; }
    public Double getAvgScore() { return avgScore; }
    public void setAvgScore(Double avgScore) { this.avgScore = avgScore; }
    public String getCategoryBreakdownJson() { return categoryBreakdownJson; }
    public void setCategoryBreakdownJson(String categoryBreakdownJson) { this.categoryBreakdownJson = categoryBreakdownJson; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
