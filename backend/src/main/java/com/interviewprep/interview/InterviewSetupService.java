package com.interviewprep.interview;

import com.interviewprep.config.AuthenticationContext;
import com.interviewprep.config.ParsingUtils;
import com.interviewprep.domain.*;
import com.interviewprep.service.TopicAutoSelectionService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service handling interview session creation, resumption, and topic resolution logic.
 * 
 * <p>Encapsulates all setup-phase concerns previously mixed into {@code InterviewController}:
 * user lookup, active session finding, topic ID/name resolution, and session persistence.</p>
 */
@Service
public class InterviewSetupService {

    private final EntityManager entityManager;
    private final InterviewSessionRepository sessionRepository;
    private final AnswerRepository answerRepository;
    private final CategoryRepository categoryRepository;
    private final AuthenticationContext authContext;
    private final TopicAutoSelectionService topicAutoSelectionService;

    public InterviewSetupService(EntityManager entityManager,
                                 InterviewSessionRepository sessionRepository,
                                 AnswerRepository answerRepository,
                                 CategoryRepository categoryRepository,
                                 AuthenticationContext authContext,
                                 TopicAutoSelectionService topicAutoSelectionService) {
        this.entityManager = entityManager;
        this.sessionRepository = sessionRepository;
        this.answerRepository = answerRepository;
        this.categoryRepository = categoryRepository;
        this.authContext = authContext;
        this.topicAutoSelectionService = topicAutoSelectionService;
    }

    /**
     * Creates a new interview session with the given parameters.
     * 
     * <p>Resolves topics (auto-select or explicit), validates language ownership,
     * and persists the active session entity.</p>
     * 
     * @param request interview start parameters including topic IDs, difficulty, count, and language ID
     * @return the newly created InterviewSession with generated question metadata
     */
    public InterviewSession createSession(InterviewStartRequest request) {
        Long userId = authContext.getCurrentUserId();
        User user = entityManager.find(User.class, userId);

        List<String> resolvedTopicIds = resolveTopicIds(request.getTopicIds(), request.getLanguageId());

        if (request.getLanguageId() != null && !request.getLanguageId().isBlank()) {
            validateTopicsForLanguage(resolvedTopicIds, request.getLanguageId());
        }

        InterviewSession session = new InterviewSession();
        session.setUser(user);
        session.setCategoryId(request.getLanguageId());
        session.setTopicIdsJson(serializeTopicIds(request.getTopicIds()));
        session.setDifficulty(request.getDifficulty());
        session.setLanguageId(request.getLanguageId() != null ? request.getLanguageId() : "");
        session.setStatus(SessionStatus.ACTIVE);
        session.setTimeoutHours(2);

        return session;
    }

    /**
     * Returns the resume status for an active interview session, if one exists.
     * 
     * @return Optional containing InterviewSummary if an active session exists, empty otherwise
     */
    public Optional<InterviewSummary> getResumeStatus() {
        Long userId = authContext.getCurrentUserId();

        return sessionRepository.findFirstByUserIdAndStatusOrderByStartedAtDesc(userId, SessionStatus.ACTIVE)
                .map(session -> {
                    int answeredCount = answerRepository.findBySessionId(session.getId()).size();
                    return InterviewSummary.from(session, answeredCount);
                });
    }

    /**
     * Resolves topic IDs for the interview start request.
     * If no topic IDs are provided, auto-selects from the selected language.
     */
    List<String> resolveTopicIds(List<String> topicIds, String languageId) {
        if (topicIds == null || topicIds.isEmpty()) {
            List<String> autoTopics = topicAutoSelectionService.autoSelectTopics(languageId);
            return autoTopics.stream()
                    .map(name -> findCategoryIdByNameAndParent(name, languageId))
                    .filter(id -> id != null)
                    .toList();
        }
        return topicIds;
    }

    private String findCategoryIdByNameAndParent(String name, String parentIdStr) {
        Long parentId = ParsingUtils.parseLongSafe(parentIdStr);
        if (parentId == null) return null;

        List<Category> topics = categoryRepository.findByParentIdAndNameContainingIgnoreCase(parentId, name);
        if (!topics.isEmpty()) {
            return String.valueOf(topics.get(0).getId());
        }
        return null;
    }

    private void validateTopicsForLanguage(List<String> topicIds, String languageId) {
        Long parentId = ParsingUtils.parseLongSafe(languageId);
        if (parentId == null) return;

        List<Category> validTopics = categoryRepository.findByParentIdAndNameContainingIgnoreCase(parentId, "");
        List<Long> validIds = validTopics.stream()
                .map(Category::getId)
                .collect(Collectors.toList());

        for (String topicId : topicIds) {
            Long id = ParsingUtils.parseLongSafe(topicId);
            if (id != null && !validIds.contains(id)) {
                throw new IllegalArgumentException("Topic ID " + topicId + " does not belong to the selected language");
            }
        }
    }

    private String serializeTopicIds(List<String> topicIds) {
        if (topicIds == null || topicIds.isEmpty()) return "";
        return "{" + String.join(",", topicIds) + "}";
    }

    List<String> resolveTopicNames(List<String> topicIds) {
        if (topicIds == null || topicIds.isEmpty()) return List.of();

        List<Long> ids = topicIds.stream()
                .map(ParsingUtils::parseLongSafe)
                .filter(id -> id != null)
                .collect(Collectors.toList());

        if (ids.isEmpty()) return List.of();

        return categoryRepository.findByIdIn(ids).stream()
                .map(Category::getName)
                .toList();
    }

    String resolveLanguageName(String languageIdStr) {
        if (languageIdStr == null || languageIdStr.isBlank()) return "";
        Long id = ParsingUtils.parseLongSafe(languageIdStr);
        if (id == null) return languageIdStr;

        Optional<Category> langCategory = categoryRepository.findById(id);
        return langCategory.map(Category::getName).orElse(languageIdStr);
    }

    String resolveTopicNamesToIds(List<String> topicNames) {
        if (topicNames == null || topicNames.isEmpty()) return "";

        List<Long> ids = categoryRepository.findByType("TOPIC").stream()
                .filter(cat -> topicNames.contains(cat.getName()))
                .map(Category::getId)
                .collect(Collectors.toList());

        return String.join(",", ids.stream().map(String::valueOf).toList());
    }
}
