package com.interviewprep.domain;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class InterviewStartRequest {
    
    /**
     * List of topic IDs to include in the interview. Can be null/empty for auto-selection.
     */
    private List<String> topicIds;
    
    /**
     * Interview difficulty level (e.g., "easy", "medium", "hard"). Must be non-blank.
     */
    @NotBlank(message = "Difficulty is required")
    private String difficulty;
    
    /**
     * Number of questions to generate for the interview. Must be between 1 and 50.
     */
    @Min(value = 1, message = "Number of questions must be at least 1")
    @Max(value = 50, message = "Number of questions cannot exceed 50")
    private int count;
    
    /**
     * Language ID for the interview. Optional - can be null for auto-selection.
     */
    private String languageId;
}
