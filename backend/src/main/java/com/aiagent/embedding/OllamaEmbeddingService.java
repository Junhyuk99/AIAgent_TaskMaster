package com.aiagent.embedding;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class OllamaEmbeddingService implements EmbeddingService {

    private final WebClient webClient;
    private final String model;
    private final int dimension;

    public OllamaEmbeddingService(
            @Value("${embedding.ollama.url:http://localhost:11434}") String baseUrl,
            @Value("${embedding.ollama.model:nomic-embed-text}") String model,
            @Value("${embedding.ollama.dimension:768}") int dimension) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.model = model;
        this.dimension = dimension;
        log.info("OllamaEmbeddingService initialized with model: {}, dimension: {}", model, dimension);
    }

    @Override
    public float[] embed(String text) {
        try {
            OllamaEmbedRequest request = new OllamaEmbedRequest();
            request.setModel(model);
            request.setPrompt(text);

            OllamaEmbedResponse response = webClient.post()
                    .uri("/api/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OllamaEmbedResponse.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response != null && response.getEmbedding() != null) {
                return toFloatArray(response.getEmbedding());
            }

            throw new RuntimeException("Empty embedding response from Ollama");
        } catch (Exception e) {
            log.error("Failed to generate embedding: {}", e.getMessage());
            throw new RuntimeException("Embedding generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> embeddings = new ArrayList<>();
        for (String text : texts) {
            embeddings.add(embed(text));
        }
        return embeddings;
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public String getModelName() {
        return model;
    }

    private float[] toFloatArray(List<Double> doubles) {
        float[] floats = new float[doubles.size()];
        for (int i = 0; i < doubles.size(); i++) {
            floats[i] = doubles.get(i).floatValue();
        }
        return floats;
    }

    @Data
    private static class OllamaEmbedRequest {
        private String model;
        private String prompt;
    }

    @Data
    private static class OllamaEmbedResponse {
        private List<Double> embedding;
    }
}
