package com.interviewprep.embedding;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

/**
 * Service wrapping Spring AI's {@link EmbeddingModel} to generate vector embeddings for text content.
 * <p>
 * This service is used during question generation to create vector representations of interview questions,
 * enabling semantic similarity search and topic-based question selection. The generated embeddings are stored
 * in PostgreSQL's pgvector extension as part of the {@link com.interviewprep.domain.Question} entity.
 * </p>
 * <h3>Purpose:</h3>
 * <ul>
 *   <li>Generate vector embeddings for interview questions to support semantic search.</li>
 *   <li>Enable topic-based question selection by comparing embedding similarity.</li>
 *   <li>Provide a zero-vector fallback when the AI service is unavailable or fails.</li>
 * </ul>
 * 
 * @see com.interviewprep.domain.Question
 * @see com.interviewprep.interview.QuestionGenerationService
 */
@Service
public class EmbeddingService {

    /** The Spring AI embedding model used to generate vector embeddings from text. */
    private final EmbeddingModel embeddingModel;

    /**
     * Constructs the embedding service with the configured {@link EmbeddingModel}.
     * 
     * @param embeddingModel the Spring AI embedding model bean (e.g., OpenAI or LM Studio)
     */
    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * Generates a vector embedding for the given text using the configured {@link EmbeddingModel}.
     * <p>
     * This is a thin wrapper around the Spring AI embedding model, providing a single entry point
     * for generating embeddings throughout the application. The returned float array represents
     * the text in high-dimensional vector space (typically 1536 dimensions for OpenAI ada-002).
     * </p>
     * 
     * @param text the input text to embed (e.g., a question's text or description)
     * @return a float array representing the vector embedding of the input text
     */
    public float[] embed(String text) {
        return embeddingModel.embed(text);
    }
}
