package com.interviewprep.embedding;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {

    // FIX-17: single source of truth for the vector column dimensionality instead of a magic 768.
    public static final int EMBEDDING_DIMENSION = 768;

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * Generate vector embeddings for the given text using the configured EmbeddingModel.
     */
    public float[] embed(String text) {
        return embeddingModel.embed(text);
    }
}
