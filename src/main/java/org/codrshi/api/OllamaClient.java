package org.codrshi.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.error.SemseaException;
import org.codrshi.metric.MetricType;
import org.codrshi.util.EmbeddingMapper;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public final class OllamaClient extends Client {

    private static final Logger log = LogManager.getLogger(OllamaClient.class);

    private static final String SERVICE_NAME = "Ollama";

    private static final String BASE_URL = "http://localhost:11434/api";
    private static final String CREATE_EMBEDDINGS = "/embed";
    private static final String EMBEDDING_MODEL = "nomic-embed-text";

    private final ObjectMapper objectMapper;

    public OllamaClient() {
        super(SERVICE_NAME);
        objectMapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    public List<List<Float>> createEmbeddings(List<String> texts) {
        log.debug("Calling Ollama to create {} embeddings.", texts.size());

        CreateEmbeddingRequest body = new CreateEmbeddingRequest(texts);
        HttpResponse<String> response = executePost(
                BASE_URL + CREATE_EMBEDDINGS,
                objectMapper.writeValueAsString(body),
                MetricType.OLLAMA_CREATE_EMBEDDINGS);

        if (response.statusCode() != 200) {
            log.error("Ollama createEmbeddings failed: status={} body={}",
                    response.statusCode(), response.body());
            throw new SemseaException("Could not generate embeddings with model '" + EMBEDDING_MODEL + "'.");
        }

        try {
            List<List<Double>> embeddings = (List<List<Double>>) objectMapper.readValue(response.body(), Map.class).get("embeddings");
            return EmbeddingMapper.map(embeddings);
        }
        catch (Exception e) {
            log.error("Ollama createEmbeddings returned malformed response: {}", response.body(), e);
            throw new SemseaException("Ollama returned an unexpected response.", e);
        }
    }

    private record CreateEmbeddingRequest(String model, List<String> input) {
        public CreateEmbeddingRequest(List<String> input) {
            this(EMBEDDING_MODEL, input);
        }
    }
}
