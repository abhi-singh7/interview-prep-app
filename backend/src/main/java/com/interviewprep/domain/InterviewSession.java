package com.interviewprep.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * JPA entity representing a mock interview session conducted by a user.
 * 
 * <p>An {@code InterviewSession} tracks the lifecycle of a single interview practice run, including its configuration
 * (category, difficulty, language), status transitions, and timing information.</p>
 * 
 * <h3>Persistence Details:</h3>
 * <ul>
 *   <li><b>Table:</b> {@code interview_sessions}</li>
 *   <li><b>ID Strategy:</b> Auto-incrementing identity column ({@link GenerationType#IDENTITY}).</li>
 *   <li><b>User Relationship:</b> Many-to-one with {@link User} — each session belongs to exactly one user.</li>
 *   <li><b>Status Tracking:</b> Enumerated via {@link SessionStatus} (e.g., ACTIVE, COMPLETED, ABANDONED).</li>
 * </ul>
 * 
 * <h3>Session Lifecycle:</h3>
 * <ol>
 *   <li>A session is created with status {@code ACTIVE} when the user starts an interview.</li>
 *   <li>Questions are generated and answered during the active phase.</li>
 *   <li>The session transitions to {@code COMPLETED} when the user finishes, or {@code ABANDONED} after a timeout period.</li>
 * </ol>
 * 
 * @see SessionStatus
 * @see InterviewSessionRepository
 */
@Entity
@Table(name = "interview_sessions")
public class InterviewSession {

    /** Auto-generated unique identifier for this session. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user who owns this interview session (lazy-loaded). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** The category ID associated with this session's questions (e.g., "data-structures"). */
    @Column(name = "category_id")
    private String categoryId;

    /** JSON string containing the list of topic IDs selected for this session. */
    @Column(name = "topic_ids_json")
    private String topicIdsJson;

    /** The difficulty level of questions in this session (e.g., "easy", "medium", "hard"). */
    private String difficulty;

    /** The programming language ID configured for this session (e.g., "java", "python"). */
    @Column(name = "language_id")
    private String languageId;

    /** Current lifecycle status of the session, tracked via {@link SessionStatus}. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    /** Timestamp when this session was created/started (auto-set on insert). */
    @CreationTimestamp
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /** Timestamp when this session ended (auto-updated on each modification, or set explicitly on completion). */
    @UpdateTimestamp
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    /** The number of hours after which an active session is considered timed out and abandoned. */
    @Column(name = "timeout_hours")
    private int timeoutHours;

    /** Default no-arg constructor required by JPA providers (e.g., Hibernate). */
    public InterviewSession() {}

    // --- Getters and Setters ---

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
