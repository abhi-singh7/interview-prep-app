package com.interviewprep.interview;

import com.interviewprep.domain.Category;
import com.interviewprep.domain.CategoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/interview")
public class LanguagesTopicsController {

    private final CategoryRepository categoryRepository;

    public LanguagesTopicsController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/languages")
    public ResponseEntity<List<LanguageResponse>> getLanguages() {
        List<Category> languages = categoryRepository.findByType("LANGUAGE");
        List<LanguageResponse> responses = languages.stream().map(lang -> 
            new LanguageResponse(String.valueOf(lang.getId()), lang.getName())
        ).toList();
        return ResponseEntity.ok(responses);
    }

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

    public record LanguageResponse(String id, String name) {}
    public record TopicResponse(String id, String name) {}
}
