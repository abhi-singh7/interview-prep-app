package com.interviewprep.output;

import java.util.List;

/**
 * Container record for a list of {@link QuestionRecord} objects returned by the AI question generation service.
 * <p>
 * OpenAI's Structured Outputs feature does not support top-level JSON arrays as native output schemas, so this
 * wrapper record is used to conform to the required object-shaped schema while still delivering an array of questions.
 * The {@code questions} field contains the actual list of generated question records.
 * </p>
 * <h3>Usage:</h3>
 * <p>This record is deserialized from AI responses in {@link com.interviewprep.question.AiGatewayService#generateQuestions(String, String, int, String, java.util.List)}
 * and then unwrapped to extract the inner list of {@link QuestionRecord} objects for mapping to domain entities.</p>
 * 
 * @see QuestionRecord
 * @see com.interviewprep.question.AiGatewayService
 */
public record QuestionRecordList(List<QuestionRecord> questions) {
}
