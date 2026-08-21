package com.interviewprep.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "interview_sessions")
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "category_id")
    private String categoryId;

    @Column(name = "topic_ids")
    private String topicIdsJson;

    private String difficulty;

    private String languageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    @CreationTimestamp
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @UpdateTimestamp
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "timeout_hours")
    private int timeoutHours;

    public InterviewSession() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getTopicIdsJson() { return topicIdsJson; }
    public void setTopicIdsJson(String topicIdsJson) { this.topicIdsJson = topicIdsJson; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getLanguageId() { return languageId; }
    public void setLanguageId(String languageId) { this.languageId = languageId; }
    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
    public int getTimeoutHours() { return timeoutHours; }
    public void setTimeoutHours(int timeoutHours) { this.timeoutHours = timeoutHours; }
}
