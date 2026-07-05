package com.interviewprep.interview;

import com.interviewprep.domain.Category;
import com.interviewprep.domain.CategoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller providing the frontend with available programming languages and their associated topics.
 * <p>
 * This controller serves as the data source for the language/topic selection UI, returning hierarchical
 * category data from the database. Languages are categories of type {@code LANGUAGE}, while topics are
 * child categories under each language parent. The frontend uses this to populate dropdown selectors
 * and search fields when users configure a new interview session.
 * </p>
 * <h3>Endpoints:</h3>
 * <ul>
 *   <li>{@code GET /interview/languages} — returns all available programming languages.</li>
 *   <li>{@code GET /interview/topics?langId=X&search=Y} — returns topics for a language, optionally filtered by search text.</li>
 * </ul>
 * 
 * @see Category
 * @see CategoryRepository
 */
@RestController
@RequestMapping("/interview")
public class LanguagesTopicsController {

    /** Repository for querying category data (languages and topics). */
    private final CategoryRepository categoryRepository;

    /**
     * Constructs the languages/topics controller with the category repository.
     * 
     * @param categoryRepository the repository providing access to language and topic categories
     */
    public LanguagesTopicsController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Returns all available programming languages as categories of type {@code LANGUAGE}.
     * <p>
     * Each language is mapped to a {@link LanguageResponse} record containing its ID and display name.
     * </p>
     * 
     * @return a list of {@link LanguageResponse} objects representing all available programming languages
     */
    @GetMapping("/languages")
    public ResponseEntity<List<LanguageResponse>> getLanguages() {
        List<Category> languages = categoryRepository.findByType("LANGUAGE");
        List<LanguageResponse> responses = languages.stream().map(lang -> 
            new LanguageResponse(String.valueOf(lang.getId()), lang.getName())
        ).toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Returns topics (child categories) for a specific language, optionally filtered by search text.
     * <p>
     * When a search query is provided and non-blank, returns only topics whose names contain the search string
     * (case-insensitive). Without a search query, returns all topics under the specified language parent,
     * ordered by type then name. Includes input length validation to prevent potential DoS from overly long queries.
     * </p>
     * 
     * @param langId  the ID of the language whose topics should be returned
     * @param search  optional search text to filter topics by name (case-insensitive); null/blank returns all topics
     * @return a list of {@link TopicResponse} objects matching the specified language and optional search criteria
     */
    @GetMapping("/topics")
    public ResponseEntity<List<TopicResponse>> getTopics(
            @RequestParam Long langId,
            @RequestParam(required = false) String search) {
        
        // Input length validation: prevent overly long search text that could cause performance issues (DoS vector)
        if (search != null && search.length() > 200) {
            return ResponseEntity.badRequest().body(List.of());
        }

        if (search != null && !search.isBlank()) {
            List<Category> topics = categoryRepository.findByParentIdAndNameContainingIgnoreCase(langId, search);
            List<TopicResponse> responses = topics.stream().map(topic -> 
                new TopicResponse(String.valueOf(topic.getId()), topic.getName())
            ).toList();
            return ResponseEntity.ok(responses);
        } else {
            List<Category> topics = categoryRepository.findByParentIdOrderByTypeThenName(langId);
            List<TopicResponse> responses = topics.stream().map(topic -> 
                new TopicResponse(String.valueOf(topic.getId()), topic.getName())
            ).toList();
            return ResponseEntity.ok(responses);
        }
    }

    /**
     * Lightweight record representing a programming language option for the frontend.
     * <p>
     * Used by {@link #getLanguages()} to return language data as simple ID-name pairs without exposing
     * the full JPA entity structure.
     * </p>
     * 
     * @param id   the string representation of the category's database ID
     * @param name the human-readable display name of the programming language
     */
    public record LanguageResponse(String id, String name) {}

    /**
     * Lightweight record representing a topic option for the frontend.
     * <p>
     * Used by the topics endpoint to return topic data as simple ID-name pairs without exposing
     * the full JPA entity structure.
     * </p>
     * 
     * @param id   the string representation of the category's database ID
     * @param name the human-readable display name of the topic
     */
    public record TopicResponse(String id, String name) {}
}
