package com.interviewprep.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Persistent record of one LLM interaction made through a ChatClient bean.
 * <p>
 * Written by {@link com.interviewprep.ai.LlmLoggingAdvisor} on every call: the full request
 * prompt (all messages, role-tagged), the response content, model name, token usage, duration and
 * status. Table is created by Hibernate at startup ({@code ddl-auto=update}); Flyway is disabled
 * for this application (see application.properties).
 */
@Entity
@Table(name = "llm_interaction_log")
public class LlmInteractionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    /** Which ChatClient bean made the call (e.g. "question-gen", "evaluation"). */
    @Column(name = "client_label", nullable = false, length = 50)
    private String clientLabel;

    /** All request messages, one per line: [SYSTEM] ... / [USER] ... */
    @Column(columnDefinition = "text")
    private String requestPrompt;

    /** Full model response text (null when the call failed before a response). */
    @Column(columnDefinition = "text")
    private String responseContent;

    @Column(length = 100)
    private String model;

    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;

    private Long durationMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LlmInteractionStatus status;

    /** Exception summary for ERROR rows. */
    @Column(columnDefinition = "text")
    private String errorMessage;

    public LlmInteractionLog() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getClientLabel() { return clientLabel; }
    public void setClientLabel(String clientLabel) { this.clientLabel = clientLabel; }

    public String getRequestPrompt() { return requestPrompt; }
    public void setRequestPrompt(String requestPrompt) { this.requestPrompt = requestPrompt; }

    public String getResponseContent() { return responseContent; }
    public void setResponseContent(String responseContent) { this.responseContent = responseContent; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Integer getPromptTokens() { return promptTokens; }
    public void setPromptTokens(Integer promptTokens) { this.promptTokens = promptTokens; }

    public Integer getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(Integer completionTokens) { this.completionTokens = completionTokens; }

    public Integer getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public LlmInteractionStatus getStatus() { return status; }
    public void setStatus(LlmInteractionStatus status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
