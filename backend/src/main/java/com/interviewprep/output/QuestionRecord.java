package com.interviewprep.output;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record QuestionRecord(
    @JsonPropertyDescription("Question type: THEORY or CODE") String type,
    @JsonPropertyDescription("List of topic names for the question") java.util.List<String> topics,
    @JsonPropertyDescription("Full question text (required for THEORY questions)") String questionText,
    @JsonPropertyDescription("Short title for coding challenges (present only for CODE questions)") String title,
    @JsonPropertyDescription("Detailed problem description for coding challenges (present only for CODE questions)") String description,
    @JsonPropertyDescription("Starter code with method signature for coding challenges (present only for CODE questions)") String codePrompt
) {
}
