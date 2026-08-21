package com.interviewprep.embedding;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {

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
