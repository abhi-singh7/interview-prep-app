package com.interviewprep.service;

import com.interviewprep.domain.Category;
import com.interviewprep.domain.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopicAutoSelectionServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private TopicAutoSelectionService topicAutoSelectionService;

    private List<Category> mockJavaTopics;

    @BeforeEach
    void setUp() {
        Category coreJava = new Category();
        coreJava.setId(11L);
        coreJava.setName("Core Java");
        coreJava.setType("TOPIC");

        Category generics = new Category();
        generics.setId(12L);
        generics.setName("Generics");
        generics.setType("TOPIC");

        mockJavaTopics = Arrays.asList(coreJava, generics);
    }

    @Test
    void autoSelectTopics_returns_all_child_topics_of_a_language() {
        when(categoryRepository.findByParentIdAndNameContainingIgnoreCase(10L, "")).thenReturn(mockJavaTopics);

        List<String> result = topicAutoSelectionService.autoSelectTopics("10");

        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder("Core Java", "Generics");
    }

    @Test
    void autoSelectTopics_returns_empty_for_invalid_language_id() {
        lenient().when(categoryRepository.findByParentIdAndNameContainingIgnoreCase(null, "")).thenReturn(List.of());

        List<String> result = topicAutoSelectionService.autoSelectTopics("invalid");

        assertThat(result).isEmpty();
    }

    @Test
    void autoSelectTopics_returns_empty_for_null_language_id() {
        lenient().when(categoryRepository.findByParentIdAndNameContainingIgnoreCase(null, "")).thenReturn(List.of());

        List<String> result = topicAutoSelectionService.autoSelectTopics(null);

        assertThat(result).isEmpty();
    }

    @Test
    void autoSelectTopics_returns_all_topics_when_search_text_is_blank() {
        when(categoryRepository.findByParentIdAndNameContainingIgnoreCase(10L, "")).thenReturn(mockJavaTopics);

        List<String> result = topicAutoSelectionService.autoSelectTopics("10");

        assertThat(result).hasSize(2);
    }
}
