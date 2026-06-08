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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class OllamaClient extends Client {

    private static final Logger log = LogManager.getLogger(OllamaClient.class);

    private static final String SERVICE_NAME = "Ollama";

    public  static final String BASE_URL = "http://localhost:11434/api";
    public  static final String EMBEDDING_MODEL = "nomic-embed-text";
    private static final String CREATE_EMBEDDINGS = "/embed";
    private static final String PS = "/ps";

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

    /**
     * Returns the set of models that Ollama currently reports as running
     * (i.e. loaded into memory). Trailing {@code :latest} tags are stripped
     * so callers can match against canonical model identifiers.
     */
    public Set<String> listRunningModels() {
        log.debug("Querying Ollama for running models (/api/ps)");
        HttpResponse<String> response = executeGet(BASE_URL + PS, MetricType.OLLAMA_PS);

        if(response.statusCode() != 200) {
            log.error("Ollama /api/ps failed: status={} body={}",
                    response.statusCode(), response.body());
            throw new SemseaException("Ollama returned status " + response.statusCode() + " for /api/ps.");
        }

        try {
            Map<String, Object> body = objectMapper.readValue(response.body(), Map.class);
            Object rawModels = body.getOrDefault("models", List.of());
            if(!(rawModels instanceof List<?> modelsList)) {
                return Set.of();
            }

            Set<String> names = new LinkedHashSet<>();
            for(Object item : modelsList) {
                if(item instanceof Map<?, ?> model) {
                    Object name = model.get("model");
                    if(name instanceof String s) {
                        names.add(stripLatestTag(s));
                    }
                }
            }
            return names;
        }
        catch (Exception e) {
            log.error("Ollama /api/ps returned malformed response: {}", response.body(), e);
            throw new SemseaException("Ollama returned an unexpected response.", e);
        }
    }

    private static String stripLatestTag(String model) {
        if(model.endsWith(":latest")) {
            return model.substring(0, model.length() - ":latest".length());
        }
        return model;
    }

    private record CreateEmbeddingRequest(String model, List<String> input) {
        public CreateEmbeddingRequest(List<String> input) {
            this(EMBEDDING_MODEL, input);
        }
    }
}
