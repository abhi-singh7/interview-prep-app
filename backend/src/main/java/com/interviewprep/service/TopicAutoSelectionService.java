package com.interviewprep.service;

import com.interviewprep.config.ParsingUtils;
import com.interviewprep.domain.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service handling automatic topic selection for interview sessions when the user does not specify topics explicitly.
 * <p>
 * When a user starts an interview without selecting specific topics, this service retrieves all available topics
 * under the chosen programming language and returns their names as a list. These topic names are then passed to
 * the AI question generation service as context for generating relevant questions.
 * </p>
 * <h3>Responsibilities:</h3>
 * <ul>
 *   <li>Resolve a language ID to its associated topic names.</li>
 *   <li>Return an empty list if the language ID is invalid or has no topics.</li>
 *   <li>Safely parse string language IDs using {@link ParsingUtils#parseLongSafe(String)}.</li>
 * </ul>
 * 
 * @see com.interviewprep.domain.CategoryRepository
 * @see com.interviewprep.config.ParsingUtils
 */
@Service
public class TopicAutoSelectionService {

    /** Repository for querying category data (topics under each language). */
    private final CategoryRepository categoryRepository;

    /**
     * Constructs the topic auto-selection service with the category repository.
     * 
     * @param categoryRepository the repository providing access to language and topic categories
     */
    public TopicAutoSelectionService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Automatically selects all topics for a given programming language by retrieving their names.
     * <p>
     * Queries the database for all child categories (topics) under the specified language parent ID,
     * then extracts and returns their display names as a list of strings. These names are used as context
     * when generating interview questions via AI.
     * </p>
     * 
     * @param languageId the string representation of the programming language's category ID
     * @return a list of topic name strings for the specified language; empty list if the language ID is invalid or has no topics
     */
    public List<String> autoSelectTopics(String languageId) {
        Long parentId = ParsingUtils.parseLongSafe(languageId);
        if (parentId == null) return List.of();
        
        List<com.interviewprep.domain.Category> topics = 
            categoryRepository.findByParentIdAndNameContainingIgnoreCase(parentId, "");
        return topics.stream()
                .map(com.interviewprep.domain.Category::getName)
                .toList();
    }

    /**
     * Safely parses a string to a Long value, returning {@code null} for invalid inputs.
     * <p>
     * This private helper is kept for potential future use or testing. The primary parsing logic uses
     * {@link ParsingUtils#parseLongSafe(String)} instead.
     * </p>
     * 
     * @param value the string to parse, may be null or blank
     * @return the parsed Long value, or null if parsing fails
     */
    @SuppressWarnings("unused") // Kept for potential future use or testing
    private Long parseLong(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
