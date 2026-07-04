package com.interviewprep.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Type;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_performance")
public class UserPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_sessions")
    private Integer totalSessions;

    @Column(name = "avg_score")
    private Double avgScore;

    // JSONB for category breakdown: { categoryId: avgScore, ... }
    @Column(columnDefinition = "jsonb")
    private String categoryBreakdownJson;

    private LocalDateTime updatedAt;

    public UserPerformance() {}

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
