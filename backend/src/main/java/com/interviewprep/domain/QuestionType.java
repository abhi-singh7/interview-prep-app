package com.interviewprep.domain;

/**
 * Enum representing the type of a technical interview question.
 * 
 * <p>This enum categorizes questions into two main types based on their format and expected response style.</p>
 * 
 * <h3>Question Types:</h3>
 * <ul>
 *   <li>{@link #THEORY} — Conceptual questions requiring written explanations (e.g., "Explain the difference between HashMap and ConcurrentHashMap").</li>
 *   <li>{@link #CODE} — Coding problems requiring implementation of a function or algorithm (e.g., "Implement binary search").</li>
 * </ul>
 * 
 * @see Question
 */
public enum QuestionType {
    /** A theoretical/conceptual question that requires a written explanation. */
    THEORY,
    
    /** A coding problem that requires writing code or an algorithm implementation. */
    CODE
}
