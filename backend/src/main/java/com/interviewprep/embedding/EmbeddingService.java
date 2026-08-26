package com.interviewprep.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {

    // FIX-17: single source of truth for the vector column dimensionality instead of a magic 768.
    public static final int EMBEDDING_DIMENSION = 768;

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * Generate vector embeddings for the given text using the configured EmbeddingModel.
     * <p>
     * Note: ChatClient advisors (LlmLoggingAdvisor) do not intercept raw {@link EmbeddingModel} calls,
     * so embedding traffic is logged here at DEBUG.
     */
    public float[] embed(String text) {
        log.debug("[LLM-embedding] REQUEST chars={}", text != null ? text.length() : 0);
        float[] vector = embeddingModel.embed(text);
        log.debug("[LLM-embedding] RESPONSE dimension={}", vector != null ? vector.length : 0);
        return vector;
    }
}
