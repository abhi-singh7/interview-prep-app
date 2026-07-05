package com.interviewprep.service;

import com.interviewprep.config.ParsingUtils;
import com.interviewprep.domain.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service providing language and topic data access for interview session configuration.
 * <p>
 * This service encapsulates the logic for retrieving programming languages and their associated topics from
 * the {@link CategoryRepository}. It is used by both the frontend-facing controllers (e.g., {@code LanguagesTopicsController})
 * and internal setup services to resolve language/topic hierarchies during interview creation.
 * </p>
 * <h3>Responsibilities:</h3>
 * <ul>
 *   <li>Retrieve all programming languages from the category database.</li>
 *   <li>Fetch topics for a specific language, with optional text-based search filtering.</li>
 *   <li>Safely parse language IDs to Long values using {@link ParsingUtils#parseLongSafe(String)}.</li>
 * </ul>
 * 
 * @see CategoryRepository
 * @see com.interviewprep.domain.Category
 */
@Service
public class LanguageTopicsService {

    /** Repository for querying category data (languages and topics). */
    private final CategoryRepository categoryRepository;

    /**
     * Constructs the language/topics service with the category repository.
     * 
     * @param categoryRepository the repository providing access to language and topic categories
     */
    public LanguageTopicsService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Retrieves all programming languages stored as categories of type {@code LANGUAGE}.
     * 
     * @return a list of {@link com.interviewprep.domain.Category} objects representing available programming languages
     */
    public List<com.interviewprep.domain.Category> getLanguages() {
        return categoryRepository.findByType("LANGUAGE");
    }

    /**
     * Retrieves topics (child categories) for a specific language, optionally filtered by search text.
     * <p>
     * If the search text is null or blank, returns all topics under the specified language parent ordered by type and name.
     * Otherwise, returns only topics whose names contain the search string (case-insensitive).
     * </p>
     * 
     * @param languageId  the ID of the language whose topics should be returned
     * @param searchText  optional search text to filter topics by name; null/blank returns all topics
     * @return a list of matching {@link com.interviewprep.domain.Category} objects, or an empty list if the language ID is invalid
     */
    public List<com.interviewprep.domain.Category> getTopicsByLanguageAndSearch(
            String languageId, String searchText) {
        Long parentId = ParsingUtils.parseLongSafe(languageId);
        if (parentId == null) return List.of();
        
        if (searchText == null || searchText.isBlank()) {
            return categoryRepository.findByParentIdOrderByTypeThenName(parentId);
        }
        return categoryRepository.findByParentIdAndNameContainingIgnoreCase(parentId, searchText);
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
