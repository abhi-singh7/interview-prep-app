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
class LanguageTopicsServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private LanguageTopicsService languageTopicsService;

    private List<Category> mockLanguages;
    private List<Category> mockJavaTopics;
    private List<Category> mockFilteredJavaTopics;

    @BeforeEach
    void setUp() {
        Category javaLang = new Category();
        javaLang.setId(10L);
        javaLang.setName("Java");
        javaLang.setType("LANGUAGE");

        Category pythonLang = new Category();
        pythonLang.setId(20L);
        pythonLang.setName("Python");
        pythonLang.setType("LANGUAGE");

        mockLanguages = Arrays.asList(javaLang, pythonLang);

        Category coreJava = new Category();
        coreJava.setId(11L);
        coreJava.setName("Core Java");
        coreJava.setType("TOPIC");

        Category generics = new Category();
        generics.setId(12L);
        generics.setName("Generics");
        generics.setType("TOPIC");

        mockJavaTopics = Arrays.asList(coreJava, generics);

        Category streams = new Category();
        streams.setId(14L);
        streams.setName("Streams");
        streams.setType("TOPIC");

        Category oop = new Category();
        oop.setId(15L);
        oop.setName("OOP");
        oop.setType("TOPIC");

        mockFilteredJavaTopics = Arrays.asList(streams, oop);
    }

    @Test
    void getLanguages_returns_only_language_type() {
        when(categoryRepository.findByType("LANGUAGE")).thenReturn(mockLanguages);

        List<Category> result = languageTopicsService.getLanguages();

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(c -> "LANGUAGE".equals(c.getType()));
        assertThat(result).extracting(Category::getName)
                .containsExactlyInAnyOrder("Java", "Python");
    }

    @Test
    void getTopicsByLanguageAndSearch_returns_all_topics_when_no_search_text() {
        when(categoryRepository.findByParentIdOrderByTypeThenName(10L)).thenReturn(mockJavaTopics);

        List<Category> result = languageTopicsService.getTopicsByLanguageAndSearch("10", "");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Category::getName)
                .containsExactlyInAnyOrder("Core Java", "Generics");
    }

    @Test
    void getTopicsByLanguageAndSearch_filters_correctly() {
        when(categoryRepository.findByParentIdAndNameContainingIgnoreCase(10L, "oo")).thenReturn(mockFilteredJavaTopics);

        List<Category> result = languageTopicsService.getTopicsByLanguageAndSearch("10", "oo");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Category::getName)
                .containsExactlyInAnyOrder("Streams", "OOP");
    }

    @Test
    void getTopicsByLanguageAndSearch_returns_empty_for_invalid_language_id() {
        lenient().when(categoryRepository.findByParentIdOrderByTypeThenName(null)).thenReturn(List.of());

        List<Category> result = languageTopicsService.getTopicsByLanguageAndSearch("invalid", "core");

        assertThat(result).isEmpty();
    }

    @Test
    void getTopicsByLanguageAndSearch_returns_empty_for_null_language_id() {
        lenient().when(categoryRepository.findByParentIdOrderByTypeThenName(null)).thenReturn(List.of());

        List<Category> result = languageTopicsService.getTopicsByLanguageAndSearch(null, "core");

        assertThat(result).isEmpty();
    }
}
