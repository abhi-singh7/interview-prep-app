package com.interviewprep.domain;

import jakarta.persistence.*;

/**
 * JPA entity representing a user's answer to a question within an interview session.
 * 
 * <p>Each {@code Answer} records the text content of the user's response along with the programming language
 * they submitted it in. It is associated with both the parent {@link InterviewSession} and the specific
 * {@link Question} that was answered.</p>
 * 
 * <h3>Persistence Details:</h3>
 * <ul>
 *   <li><b>Table:</b> {@code answers}</li>
 *   <li><b>ID Strategy:</b> Auto-incrementing identity column ({@link GenerationType#IDENTITY}).</li>
 *   <li><b>Session Relationship:</b> Many-to-one with {@link InterviewSession}.</li>
 *   <li><b>Question Relationship:</b> Many-to-one with {@link Question}.</li>
 * </ul>
 * 
 * @see InterviewSession
 * @see Question
 */
@Entity
@Table(name = "answers")
public class Answer {

    /** Auto-generated unique identifier for this answer. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The interview session this answer belongs to (lazy-loaded). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession session;

    /** The question that was answered (lazy-loaded). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    /** The text content of the user's answer. Stored as TEXT to support long-form responses. */
    @Column(columnDefinition = "text")
    private String answerText;

    /** The programming language ID the user selected when submitting this answer (e.g., "java", "python"). */
    private String languageSubmitted;

    /** Default no-arg constructor required by JPA providers (e.g., Hibernate). */
    public Answer() {}

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public InterviewSession getSession() { return session; }
    public void setSession(InterviewSession session) { this.session = session; }
    
    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }
    
    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
    
    public String getLanguageSubmitted() { return languageSubmitted; }
    public void setLanguageSubmitted(String languageSubmitted) { this.languageSubmitted = languageSubmitted; }
}
