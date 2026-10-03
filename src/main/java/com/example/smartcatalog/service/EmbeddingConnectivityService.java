package com.example.smartcatalog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Thin verification service that injects {@link EmbeddingModel} and exercises
 * the Gemini Embeddings API (gemini-embedding-001) to confirm end-to-end connectivity.
 *
 * <p>Remove or replace this service once the real embedding logic is in place.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingConnectivityService {

    /** Auto-configured by spring-ai-starter-model-google-genai-embedding. */
    private final EmbeddingModel embeddingModel;

    /**
     * Sends a single probe string to the OpenAI Embeddings endpoint and returns
     * the dimension count of the resulting vector.
     *
     * @return the number of dimensions in the returned embedding vector
     */
    public int probeEmbeddingDimensions() {
        EmbeddingResponse response = embeddingModel.embedForResponse(
                List.of("SmartCatalog connectivity probe"));

        int dimensions = response.getResults().getFirst().getOutput().length;
        log.info("[EmbeddingConnectivityService] OpenAI embedding returned {} dimensions.", dimensions);
        return dimensions;
    }
}
