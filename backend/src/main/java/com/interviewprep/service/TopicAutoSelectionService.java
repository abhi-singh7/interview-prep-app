package com.interviewprep.service;

import com.interviewprep.config.ParsingUtils;
import com.interviewprep.domain.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TopicAutoSelectionService {

    private final CategoryRepository categoryRepository;

    public TopicAutoSelectionService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<String> autoSelectTopics(String languageId) {
        Long parentId = ParsingUtils.parseLongSafe(languageId);
        if (parentId == null) return List.of();
        
        List<com.interviewprep.domain.Category> topics = 
            categoryRepository.findByParentIdAndNameContainingIgnoreCase(parentId, "");
        return topics.stream()
                .map(com.interviewprep.domain.Category::getName)
                .toList();
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
