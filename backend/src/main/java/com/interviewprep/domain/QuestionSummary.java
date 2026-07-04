package com.interviewprep.domain;

/**
 * Lightweight DTO for transferring question data between backend and frontend.
 * 
 * <p>This record provides a minimal representation of a Question entity, containing only the
 * essential fields needed for list views or summary displays.</p>
 * 
 * <p>Used in endpoints that return question collections without full details:</p>
 * <ul>
 *   <li>{@code GET /interview/session/{sessionId}/questions}</li>
 * </ul>
 */
public record QuestionSummary(
        Long id,
        String type,
        String categoryId,
        String questionText
) {

    /**
     * Creates a QuestionSummary from an existing Question entity.
     * 
     * @param question the source Question entity to extract data from
     * @return a new QuestionSummary instance with values from the entity
     */
    public static QuestionSummary from(Question question) {
        return new QuestionSummary(
                question.getId(),
                question.getType() != null ? question.getType().name() : "THEORY",
                question.getCategoryId(),
                question.getQuestionText()
        );
    }
}
