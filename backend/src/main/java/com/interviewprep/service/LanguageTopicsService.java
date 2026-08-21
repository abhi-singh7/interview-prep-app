package com.interviewprep.service;

import com.interviewprep.config.ParsingUtils;
import com.interviewprep.domain.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LanguageTopicsService {

    private final CategoryRepository categoryRepository;

    public LanguageTopicsService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<com.interviewprep.domain.Category> getLanguages() {
        return categoryRepository.findByType("LANGUAGE");
    }

    public List<com.interviewprep.domain.Category> getTopicsByLanguageAndSearch(
            String languageId, String searchText) {
        Long parentId = ParsingUtils.parseLongSafe(languageId);
        if (parentId == null) return List.of();
        
        if (searchText == null || searchText.isBlank()) {
            return categoryRepository.findByParentIdOrderByTypeThenName(parentId);
        }
        return categoryRepository.findByParentIdAndNameContainingIgnoreCase(parentId, searchText);
    }

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
