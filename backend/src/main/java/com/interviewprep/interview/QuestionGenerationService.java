package com.interviewprep.interview;

import com.interviewprep.domain.*;
import com.interviewprep.embedding.EmbeddingService;
import com.interviewprep.output.QuestionRecord;
import com.interviewprep.question.AiGatewayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service handling AI-powered question generation and vector embedding for interview questions.
 * 
 * <p>Encapsulates the complete question lifecycle: invoking AiGatewayService to generate questions,
 * creating Question entities with topic metadata, generating vector embeddings via EmbeddingService,
 * and persisting all generated questions linked to a session.</p>
 */
@Service
public class QuestionGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(QuestionGenerationService.class);

    private final AiGatewayService aiGatewayService;
    private final EmbeddingService embeddingService;
    private final QuestionRepository questionRepository;
    private final InterviewSessionRepository sessionRepository;
    private final InterviewSetupService interviewSetupService;

    public QuestionGenerationService(AiGatewayService aiGatewayService,
                                     EmbeddingService embeddingService,
                                     QuestionRepository questionRepository,
                                     InterviewSessionRepository sessionRepository,
                                     InterviewSetupService interviewSetupService) {
        this.aiGatewayService = aiGatewayService;
        this.embeddingService = embeddingService;
        this.questionRepository = questionRepository;
        this.sessionRepository = sessionRepository;
        this.interviewSetupService = interviewSetupService;
    }

    /**
     * Generates questions for a new interview session via AI and persists them.
     * 
     * <p>This method orchestrates the full question generation pipeline:
     * 1) Calls AiGatewayService to generate QUESTION_RECORD objects
     * 2) Maps each record to a Question entity with type, text, title/description/codePrompt
     * 3) Generates vector embeddings (with zero-vector fallback on failure)
     * 4) Persists all questions linked to the given session</p>
     * 
     * @param sessionId the interview session ID to link generated questions to
     * @param topicNames list of topic names for AI prompt context
     * @param languageNameForPrompt the programming language name for coding challenge prompts
     * @param difficulty the difficulty level string (e.g., "easy", "medium", "hard")
     * @param count number of questions to generate
     * @return list of persisted Question entities
     */
    public List<Question> generateAndPersistQuestions(Long sessionId, List<String> topicNames,
                                                       String languageNameForPrompt, String difficulty, int count) {
        List<QuestionRecord> questionRecords = aiGatewayService.generateQuestions(
                "", difficulty, count, languageNameForPrompt, topicNames);

        InterviewSession session = sessionRepository.findById(sessionId).orElse(null);

        List<Question> questions = new ArrayList<>();

        for (QuestionRecord record : questionRecords) {
            Question q = new Question();
            q.setId(null); // Let JPA assign ID on save
            q.setType(record.type() != null ? QuestionType.valueOf(record.type()) : null);

            if (record.topics() != null && !record.topics().isEmpty()) {
                String resolvedId = interviewSetupService.resolveTopicNamesToIds(record.topics());
                q.setCategoryId(resolvedId);
            } else {
                q.setCategoryId(null);
            }

            q.setQuestionText(record.questionText());
            if (record.title() != null) q.setTitle(record.title());
            if (record.description() != null) q.setDescription(record.description());
            if (record.codePrompt() != null) q.setCodePrompt(record.codePrompt());

            // Link question to session for DB persistence and retrieval
            if (session != null) {
                q.setSession(session);
            }

            // Generate vector embedding for the question text (Hibernate 7 native VECTOR type)
            String textToEmbed = record.questionText();
            if (textToEmbed != null && !textToEmbed.isBlank()) {
                try {
                    float[] embedding = embeddingService.embed(textToEmbed);
                    double[] doubleArray = new double[embedding.length];
                    for (int i = 0; i < embedding.length; i++) {
                        doubleArray[i] = (double) embedding[i];
                    }
                    q.setVectorEmbedding(doubleArray);
                } catch (Exception e) {
                    // Fallback: store zero-vector when embedding service is unavailable.
                    // This ensures question persistence succeeds even without vector support,
                    // allowing the interview to proceed (just without similarity search capability).
                    logger.warn("Failed to generate vector embedding for question: {}. Using zero-vector fallback.", textToEmbed, e);
                    q.setVectorEmbedding(new double[768]);
                }
            } else {
                q.setVectorEmbedding(new double[768]);
            }

            questions.add(q);
        }

        // Persist generated questions with session reference
        for (Question q : questions) {
            questionRepository.save(q);
        }

        return questions;
    }
}
