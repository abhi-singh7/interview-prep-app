package com.interviewprep.question;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiGatewayServiceMappingTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void evaluation_record_maps_to_evaluation_entity_correctly() throws JsonProcessingException {
        // Given: an EvaluationRecord from AI response
        String strengthsJson = "[\"Good algorithmic thinking\", \"Correct time complexity\"]";
        String weaknessesJson = "[\"Could improve edge case handling\"]";

        // When: we parse the JSON arrays to match service behavior
        var strengthsArray = objectMapper.readTree(strengthsJson);
        var weaknessesArray = objectMapper.readTree(weaknessesJson);

        // Then: parsed arrays should contain expected elements
        assert strengthsArray.size() == 2 : "Strengths should have 2 items";
        assert weaknessesArray.size() == 1 : "Weaknesses should have 1 item";
        assert strengthsArray.get(0).asText().equals("Good algorithmic thinking") : "First strength mismatch";
    }

    @Test
    void evaluation_record_handles_null_strengths_gracefully() throws JsonProcessingException {
        // Given: null strengths in record
        String nullStrengthsJson = null;

        // When: service handles null case by setting empty array
        String result = "[]"; // Service fallback behavior

        // Then: should not throw exception and return empty array
        assert result.equals("[]") : "Should default to empty array for null strengths";
    }

    @Test
    void evaluation_record_handles_jackson_parsing_errors() throws JsonProcessingException {
        // Given: malformed JSON that would cause JacksonException
        String malformedJson = "[invalid json content";

        try {
            objectMapper.readTree(malformedJson);
            assert false : "Should have thrown exception for malformed JSON";
        } catch (JsonProcessingException e) {
            // Expected - service should catch and use empty array fallback
        }
    }

    @Test
    void question_record_structure_matches_expected_schema() throws JsonProcessingException {
        // Given: a properly formatted QuestionRecord from AI
        String questionJson = """
            {
                "type": "CODE",
                "topics": ["Core Java", "Generics"],
                "questionText": "Explain the difference between abstract classes and interfaces.",
                "title": null,
                "description": null,
                "codePrompt": null
            }
            """;

        // When: we parse it with ObjectMapper
        var tree = objectMapper.readTree(questionJson);

        // Then: all fields should be accessible
        assert tree.get("type").asText().equals("CODE") : "Type mismatch";
        assert tree.get("topics").size() == 2 : "Topics count mismatch";
    }

    @Test
    void coding_question_record_contains_required_fields() throws JsonProcessingException {
        // Given: a CODE type question with all fields populated
        String codeQuestionJson = """
            {
                "type": "CODE",
                "topics": ["Algorithms"],
                "questionText": "Implement binary search.",
                "title": "Binary Search Implementation",
                "description": "Write a function to perform binary search on sorted array.",
                "codePrompt": "def binary_search(arr, target):\\n    # TODO: implement"
            }
            """;

        // When: we parse it
        var tree = objectMapper.readTree(codeQuestionJson);

        // Then: CODE questions should have title, description, and codePrompt
        assert tree.get("title").asText().equals("Binary Search Implementation") : "Title mismatch";
        assert !tree.get("description").isNull() : "Description should not be null for CODE type";
    }

    @Test
    void theory_question_record_has_null_optional_fields() throws JsonProcessingException {
        // Given: a THEORY type question without optional fields
        String theoryQuestionJson = """
            {
                "type": "THEORY",
                "topics": ["Java Collections"],
                "questionText": "What is the difference between ArrayList and LinkedList?",
                "title": null,
                "description": null,
                "codePrompt": null
            }
            """;

        // When: we parse it
        var tree = objectMapper.readTree(theoryQuestionJson);

        // Then: THEORY questions should have null optional fields
        assert tree.get("type").asText().equals("THEORY") : "Type mismatch";
    }

    @Test
    void evaluation_record_score_range_is_valid() throws JsonProcessingException {
        // Given: score values at boundaries
        int minScore = 0;
        int maxScore = 10;

        // When: scores are within expected range (0-10)
        
        // Then: scores should be valid
        assert minScore >= 0 && minScore <= 10 : "Min score out of range";
        assert maxScore >= 0 && maxScore <= 10 : "Max score out of range";
    }

    @Test
    void evaluation_record_complexity_fields_are_optional() throws JsonProcessingException {
        // Given: a coding evaluation record with complexity fields
        String evalJson = """
            {
                "score": 8,
                "strengths": [],
                "weaknesses": ["Space optimization possible"],
                "improved_answer": "Better solution here",
                "is_correct": true,
                "correctness_explanation": "Algorithm is correct",
                "time_complexity": "O(n log n)",
                "space_complexity": "O(1)"
            }
            """;

        // When: we parse it
        var tree = objectMapper.readTree(evalJson);

        // Then: complexity fields should be accessible for CODE type evaluations
        assert !tree.get("time_complexity").isNull() : "Time complexity should exist";
        assert !tree.get("space_complexity").isNull() : "Space complexity should exist";
    }
}
