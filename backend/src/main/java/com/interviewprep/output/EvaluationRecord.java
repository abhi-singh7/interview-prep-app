package com.interviewprep.output;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record EvaluationRecord(
    @JsonPropertyDescription("Score rating of 0-10 for answer quality") int score,
    @JsonPropertyDescription("What the user did well") java.util.List<String> strengths,
    @JsonPropertyDescription("Areas for improvement") java.util.List<String> weaknesses,
    @JsonPropertyDescription("Better version of the answer or improved solution code") String improvedAnswer,
    @JsonPropertyDescription("Whether the coding solution is algorithmically correct (null for theory evaluation)") Boolean isCorrect,
    @JsonPropertyDescription("Explanation of correctness assessment (null for theory evaluation)") String correctnessExplanation,
    @JsonPropertyDescription("Algorithmic time complexity in Big-O notation (null for theory evaluation)") String timeComplexity,
    @JsonPropertyDescription("Algorithmic space complexity in Big-O notation (null for theory evaluation)") String spaceComplexity
) {
}
