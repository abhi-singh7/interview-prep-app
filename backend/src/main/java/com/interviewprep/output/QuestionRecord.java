package com.interviewprep.output;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Structured output record representing a single AI-generated interview question.
 * <p>
 * This record is used as the deserialized target for Spring AI's native structured output when generating
 * interview questions via {@link com.interviewprep.question.AiGatewayService}. The AI model returns JSON objects
 * matching this schema, which are then mapped to {@link com.interviewprep.domain.Question} entities.
 * </p>
 * <h3>Fields:</h3>
 * <ul>
 *   <li>{@code type} — question classification: {@code THEORY} or {@code CODE}.</li>
 *   <li>{@code topics} — list of topic names associated with this question.</li>
 *   <li>{@code questionText} — full question text (required for THEORY questions).</li>
 *   <li>{@code title} — short title for coding challenges (present only for CODE questions).</li>
 *   <li>{@code description} — detailed problem description for coding challenges.</li>
 *   <li>{@code codePrompt} — starter code with method signature for coding challenges.</li>
 * </ul>
 * 
 * @see QuestionRecordList
 * @see com.interviewprep.domain.Question
 */
public record QuestionRecord(
    /** Question type: THEORY or CODE. */
    @JsonPropertyDescription("Question type: THEORY or CODE") String type,
    /** List of topic names for the question (e.g., ["arrays", "sorting"]). */
    @JsonPropertyDescription("List of topic names for the question") java.util.List<String> topics,
    /** Full question text (required for THEORY questions). */
    @JsonPropertyDescription("Full question text (required for THEORY questions)") String questionText,
    /** Short title for coding challenges (present only for CODE questions). */
    @JsonPropertyDescription("Short title for coding challenges (present only for CODE questions)") String title,
    /** Detailed problem description for coding challenges (present only for CODE questions). */
    @JsonPropertyDescription("Detailed problem description for coding challenges (present only for CODE questions)") String description,
    /** Starter code with method signature for coding challenges (present only for CODE questions). */
    @JsonPropertyDescription("Starter code with method signature for coding challenges (present only for CODE questions)") String codePrompt
) {
}
