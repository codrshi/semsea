package org.codrshi.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.metric.MetricType;
import org.codrshi.util.EmbeddingMapper;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public final class OllamaClient extends Client {

    private final static Logger log = LogManager.getLogger(OllamaClient.class.getName());

    private static final String BASE_URL  = "http://localhost:11434/api";
    private static final String CREATE_EMBEDDINGS = "/embed";
    private static final String EMBEDDING_MODEL = "nomic-embed-text";
    private final ObjectMapper objectMapper;

    public OllamaClient() {
        super();
        objectMapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    public List<List<Float>> createEmbeddings(List<String> texts){
        log.debug("Calling Ollama for creating {} embeddings.", texts.size());

        CreateEmbeddingRequest createEmbeddingRequest = new CreateEmbeddingRequest(texts);

        String requestUrl = BASE_URL + CREATE_EMBEDDINGS;
        String requestBody = objectMapper.writeValueAsString(createEmbeddingRequest);

        log.debug("Request body: {}", requestBody);
        HttpResponse<String> response = executePost(requestUrl, requestBody, MetricType.OLLAMA_CREATE_EMBEDDINGS);

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to create embedding: " + response.body());
        }

        log.debug("Created {} embeddings successfully.", texts.size());
        List<List<Double>> embeddings = (List<List<Double>>) objectMapper.readValue(response.body(), Map.class).get("embeddings");

        return EmbeddingMapper.map(embeddings);
    }

    private record CreateEmbeddingRequest(String model, List<String> input){
        public CreateEmbeddingRequest(List<String> input){
            this(EMBEDDING_MODEL, input);
        }
    }
}
